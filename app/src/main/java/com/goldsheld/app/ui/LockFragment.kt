package com.goldsheld.app.ui

import android.Manifest
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.goldsheld.app.Graph
import com.goldsheld.app.R
import com.goldsheld.app.data.db.SafeZone
import com.goldsheld.app.databinding.FragmentLockBinding
import com.goldsheld.app.databinding.ItemPlaceRowBinding
import com.goldsheld.app.domain.CombineRule
import com.goldsheld.app.domain.LockEvaluator
import com.goldsheld.app.domain.LockManager
import com.goldsheld.app.domain.LockMode
import com.goldsheld.app.domain.LocationKind
import com.goldsheld.app.domain.ModuleSnapshot
import com.goldsheld.app.location.LocationHelper
import kotlinx.coroutines.launch

/**
 * Lock setup: condition (time / location / both), time chips, saved place, ANY / ALL rule.
 * Starting a lock really starts one via [LockManager]. Goldsheld's location mode means
 * arriving at a saved zone ([LocationKind.ZONE]); the distance-travel unlock the engine
 * also supports has no control in this UI.
 */
class LockFragment : Fragment() {

    private var _binding: FragmentLockBinding? = null
    private val binding get() = _binding!!

    private val modeNames = listOf("সময়", "লোকেশন", "সময় + লোকেশন")
    private val timeNames = listOf("৩০ মিনিট", "১ ঘণ্টা", "৩ ঘণ্টা", "৬ ঘণ্টা", "১২ ঘণ্টা", "২৪ ঘণ্টা", "কাস্টম")
    private val timePresetsMs = listOf(30L, 60L, 180L, 360L, 720L, 1440L).map { it * 60_000L }

    private var zones: List<SafeZone> = emptyList()
    private var mode = 2
    private var time = 3
    private var customMinutes: Long = 60
    /** Index into [zones]; the "add a place" row is not selectable. */
    private var place = 0
    private var ruleAny = true
    private var startPending = false

    private val modeViews = ArrayList<TextView>()
    private val timeViews = ArrayList<TextView>()
    private val placeViews = ArrayList<ItemPlaceRowBinding>()

    private val selectedZone: SafeZone? get() = zones.getOrNull(place)

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (startPending) {
                startPending = false
                doStartLock()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        modeViews.clear(); timeViews.clear(); placeViews.clear()

        modeNames.forEachIndexed { i, name ->
            val tv = TextView(ctx)
            tv.text = name
            tv.gravity = android.view.Gravity.CENTER
            tv.minHeight = ctx.dp(44)
            tv.textSize = 14f
            tv.setTypeface(null, android.graphics.Typeface.BOLD)
            tv.isClickable = true
            tv.isFocusable = true
            tv.setOnClickListener { mode = i; refresh() }
            binding.modes.addView(tv, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            modeViews.add(tv)
        }
        timeNames.forEachIndexed { i, name ->
            val pill = ctx.pill(name, ChipStyle.UNSELECTED, 44)
            pill.isClickable = true
            pill.isFocusable = true
            pill.setOnClickListener {
                if (i == timeNames.lastIndex) showCustomTimeDialog() else {
                    time = i
                    refresh()
                }
            }
            binding.times.addView(pill)
            timeViews.add(pill)
        }
        binding.ruleAny.setOnClickListener { ruleAny = true; refresh() }
        binding.ruleAll.setOnClickListener { ruleAny = false; refresh() }
        binding.btnStart.setOnClickListener { onStartClicked() }
        renderPlaces()
        viewLifecycleOwner.lifecycleScope.launch {
            zones = Graph.db.safeZoneDao().getAll()
            place = 0
            renderPlaces()
        }
    }

    private fun renderPlaces() {
        val ctx = requireContext()
        binding.places.removeAllViews()
        placeViews.clear()
        zones.forEachIndexed { i, zone ->
            val row = ItemPlaceRowBinding.inflate(layoutInflater, binding.places, false)
            row.name.text = zone.name
            row.sub.text = "ব্যাসার্ধ ${zone.radiusM.bn()} মি"
            row.root.setOnClickListener { place = i; refresh() }
            binding.places.addView(row.root)
            placeViews.add(row)
        }
        // "Add a place" row: not a selectable unlock target — it points at the Location tab.
        val add = ItemPlaceRowBinding.inflate(layoutInflater, binding.places, false)
        add.name.text = "নতুন জায়গা যোগ করুন"
        add.sub.text = "বর্তমান অবস্থান থেকে"
        add.radio.setImageResource(R.drawable.ic_location)
        add.radio.setColorFilter(ctx.color(R.color.accent))
        add.root.setOnClickListener {
            Toast.makeText(ctx, R.string.ui_add_zone_hint, Toast.LENGTH_SHORT).show()
        }
        binding.places.addView(add.root)
        placeViews.add(add)
        refresh()
    }

    private fun showCustomTimeDialog() {
        val ctx = requireContext()
        val input = EditText(ctx).apply {
            hint = getString(R.string.ui_custom_time_hint)
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(customMinutes.toString())
            setPadding(ctx.dp(16), ctx.dp(12), ctx.dp(16), ctx.dp(12))
        }
        AlertDialog.Builder(ctx)
            .setTitle(R.string.ui_custom_time_title)
            .setView(input)
            .setPositiveButton(R.string.ui_ok) { _, _ ->
                val minutes = input.text.toString().toLongOrNull()
                if (minutes == null || !LockEvaluator.isValidCustomMinutes(minutes)) {
                    Toast.makeText(ctx, R.string.ui_invalid_minutes, Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                customMinutes = minutes
                time = timeNames.lastIndex
                refresh()
            }
            .setNegativeButton(R.string.ui_cancel, null)
            .show()
    }

    private fun onStartClicked() {
        viewLifecycleOwner.lifecycleScope.launch {
            val ctx = requireContext()
            if (Graph.lockManager.isActive()) {
                Toast.makeText(ctx, R.string.ui_lock_already_active, Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (mode != 0 && selectedZone == null) {
                Toast.makeText(ctx, R.string.ui_no_zones, Toast.LENGTH_LONG).show()
                return@launch
            }
            // A zone lock needs location to notice the arrival; ask first, start either way
            // (the integrity check warns when permission is missing, like the reference).
            if (mode != 0 && !LocationHelper.hasForegroundPermission(ctx)) {
                startPending = true
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                )
                return@launch
            }
            doStartLock()
        }
    }

    private fun doStartLock() {
        viewLifecycleOwner.lifecycleScope.launch {
            val ctx = requireContext()
            val settings = Graph.settings.get()
            val lockMode = when (mode) {
                0 -> LockMode.TIME
                1 -> LockMode.DISTANCE
                else -> LockMode.COMBINED
            }
            val durationMs = if (lockMode.usesTime) {
                if (time == timeNames.lastIndex) customMinutes * 60_000L else timePresetsMs[time]
            } else 0L
            val zone = if (lockMode.usesDistance) selectedZone else null
            val result = Graph.lockManager.start(
                LockManager.StartRequest(
                    mode = lockMode,
                    durationMs = durationMs,
                    distanceM = 0,
                    rule = if (ruleAny) CombineRule.ANY else CombineRule.ALL,
                    startLat = null,
                    startLng = null,
                    blockAdult = true,
                    blockApps = true,
                    locationKind = LocationKind.ZONE,
                    unlockZoneId = zone?.id ?: 0,
                    modules = ModuleSnapshot.fromSettings(settings),
                    blockIncognito = settings.blockIncognito,
                    imageSensitivity = if (settings.imageDetection) settings.imageSensitivity else 0
                )
            )
            when (result) {
                LockManager.StartResult.Started -> {
                    Toast.makeText(ctx, R.string.ui_lock_started, Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.switchTab(R.id.nav_home)
                }
                LockManager.StartResult.AlreadyActive ->
                    Toast.makeText(ctx, R.string.ui_lock_already_active, Toast.LENGTH_SHORT).show()
                LockManager.StartResult.NeedsUnlockZone ->
                    Toast.makeText(ctx, R.string.ui_no_zones, Toast.LENGTH_LONG).show()
                LockManager.StartResult.InvalidDuration ->
                    Toast.makeText(ctx, R.string.ui_invalid_minutes, Toast.LENGTH_LONG).show()
                else ->
                    Toast.makeText(ctx, R.string.ui_lock_start_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refresh() {
        val ctx = requireContext()
        modeViews.forEachIndexed { i, tv ->
            if (i == mode) {
                tv.setBackgroundResource(R.drawable.bg_segment_selected)
                tv.setTextColor(ctx.color(R.color.on_accent))
            } else {
                tv.background = null
                tv.setTextColor(ctx.color(R.color.text))
            }
        }
        timeViews.forEachIndexed { i, tv -> ctx.applyPill(tv, if (i == time) ChipStyle.SELECTED else ChipStyle.UNSELECTED) }
        placeViews.forEachIndexed { i, row ->
            if (i == zones.size) {
                // The "add a place" row keeps its location icon.
                row.radio.setImageResource(R.drawable.ic_location)
                row.radio.setColorFilter(ctx.color(R.color.accent))
            } else {
                row.radio.clearColorFilter()
                row.radio.setImageResource(if (i == place) R.drawable.radio_on else R.drawable.radio_off)
            }
        }
        binding.ruleAny.setBackgroundResource(if (ruleAny) R.drawable.bg_option_selected else R.drawable.bg_option)
        binding.ruleAll.setBackgroundResource(if (!ruleAny) R.drawable.bg_option_selected else R.drawable.bg_option)

        val timeText = if (time == timeNames.lastIndex) "${customMinutes.bn()} মিনিট" else timeNames[time]
        val zone = selectedZone
        val before = "তার আগে পাওয়ার মেনু, অ্যাপ, সাইট ও ইনস্টল বন্ধ থাকবে।"
        val lead = when {
            mode == 0 -> "$timeText পার হলে লক খুলবে।"
            zone == null -> "লোকেশন লকের জন্য প্রথমে একটি জায়গা যোগ করতে হবে।"
            mode == 1 -> "${zone.name}-এ পৌঁছালে লক খুলবে।"
            else -> if (ruleAny) "$timeText পার হলে অথবা ${zone.name}-এ পৌঁছালে লক খুলবে।"
            else "$timeText পার হলে এবং ${zone.name}-এ পৌঁছালে লক খুলবে।"
        }
        binding.summary.text = if (mode != 0 && zone == null) "সারাংশ: $lead" else "সারাংশ: $lead $before"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
