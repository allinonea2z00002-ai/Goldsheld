package com.goldsheld.app.ui

import android.Manifest
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.goldsheld.app.Graph
import com.goldsheld.app.R
import com.goldsheld.app.data.db.SafeZone
import com.goldsheld.app.databinding.ItemPlaceRowBinding
import com.goldsheld.app.location.LocationHelper
import kotlinx.coroutines.launch

/**
 * Location tab: the real saved zones from the database. "+ নতুন জায়গা যোগ করুন"
 * takes the current location, asks for a name and radius, and saves the zone.
 * "লোকেশন ইতিহাস মুছুন" deletes every stored [com.goldsheld.app.data.db.LocationRecord]
 * (the fixes the lock engine recorded); it does not delete the saved zones.
 * Long-press a zone to delete it — except the unlock zone of a running lock.
 */
class LocationFragment : CardsFragment() {
    override val titleRes = R.string.loc_title
    override val subtitleRes = R.string.loc_sub
    override fun bottomButtonText(): String = getString(R.string.loc_add)

    private var zoneCard: LinearLayout? = null
    private var zones: List<SafeZone> = emptyList()
    private var addPending = false

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!addPending) return@registerForActivityResult
            addPending = false
            if (granted) startAddZone()
            else Toast.makeText(requireContext(), R.string.ui_location_permission, Toast.LENGTH_LONG).show()
        }

    override fun buildList(list: LinearLayout) {
        val ctx = requireContext()
        val heading = TextView(ctx)
        heading.setText(R.string.loc_zones)
        heading.setTextColor(ctx.color(R.color.muted))
        heading.textSize = 13f
        list.addView(heading)

        val card = LinearLayout(ctx)
        card.orientation = LinearLayout.VERTICAL
        card.setBackgroundResource(R.drawable.bg_card)
        card.setPadding(ctx.dp(14), ctx.dp(4), ctx.dp(14), ctx.dp(4))
        zoneCard = card
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = ctx.dp(8)
        lp.bottomMargin = ctx.dp(14)
        list.addView(card, lp)
        renderZones()

        val clear = TextView(ctx)
        clear.setText(R.string.loc_clear)
        clear.setTextColor(ctx.color(R.color.danger))
        clear.textSize = 14f
        clear.setTypeface(null, android.graphics.Typeface.BOLD)
        clear.minHeight = ctx.dp(44)
        clear.gravity = android.view.Gravity.CENTER_VERTICAL
        clear.isClickable = true
        clear.isFocusable = true
        clear.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                Graph.db.locationDao().deleteAll()
                Toast.makeText(ctx, R.string.ui_history_cleared, Toast.LENGTH_SHORT).show()
            }
        }
        list.addView(clear)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            zones = Graph.db.safeZoneDao().getAll()
            renderZones()
        }
    }

    private fun renderZones() {
        val card = zoneCard ?: return
        val ctx = requireContext()
        card.removeAllViews()
        if (zones.isEmpty()) {
            val empty = TextView(ctx)
            empty.text = "কোনো জায়গা সেভ করা নেই"
            empty.setTextColor(ctx.color(R.color.muted))
            empty.textSize = 14f
            empty.setPadding(0, ctx.dp(10), 0, ctx.dp(10))
            card.addView(empty)
            return
        }
        zones.forEach { zone ->
            val row = ItemPlaceRowBinding.inflate(layoutInflater, card, false)
            row.radio.setImageResource(R.drawable.ic_location)
            row.radio.setColorFilter(ctx.color(R.color.accent))
            row.name.text = zone.name
            row.sub.text = buildString {
                append("ব্যাসার্ধ ${zone.radiusM.bn()} মি")
                if (zone.autoLockMinutes > 0) append(" · আসার সময় ${zone.autoLockMinutes.bn()} মিনিটের লক")
            }
            row.root.setOnLongClickListener {
                showDeleteZoneDialog(zone)
                true
            }
            card.addView(row.root)
        }
    }

    override fun onBottomButton() {
        if (LocationHelper.hasForegroundPermission(requireContext())) {
            startAddZone()
        } else {
            addPending = true
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun startAddZone() {
        viewLifecycleOwner.lifecycleScope.launch {
            val ctx = requireContext()
            val fix = LocationHelper.currentFix(ctx, highAccuracy = true)
            if (fix == null) {
                Toast.makeText(ctx, R.string.ui_location_unavailable, Toast.LENGTH_LONG).show()
                return@launch
            }
            showAddZoneDialog(fix.latitude, fix.longitude)
        }
    }

    private fun showAddZoneDialog(lat: Double, lng: Double) {
        val ctx = requireContext()
        val nameInput = EditText(ctx).apply { hint = getString(R.string.ui_zone_name_hint) }
        val radiusInput = EditText(ctx).apply {
            hint = getString(R.string.ui_zone_radius_hint)
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(16), ctx.dp(8), ctx.dp(16), 0)
            addView(nameInput)
            addView(radiusInput)
        }
        AlertDialog.Builder(ctx)
            .setTitle(R.string.ui_zone_title)
            .setView(container)
            .setPositiveButton(R.string.ui_add) { _, _ ->
                val name = nameInput.text.toString().trim()
                val radius = radiusInput.text.toString().toIntOrNull()
                if (name.isEmpty() || radius == null || radius !in 100..5000) {
                    Toast.makeText(ctx, R.string.ui_zone_invalid, Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    Graph.db.safeZoneDao().upsert(
                        SafeZone(name = name, latitude = lat, longitude = lng, radiusM = radius)
                    )
                    zones = Graph.db.safeZoneDao().getAll()
                    renderZones()
                    runCatching { Graph.geofences.sync(Graph.lockManager.current(), zones) }
                    Toast.makeText(ctx, R.string.ui_zone_added, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.ui_cancel, null)
            .show()
    }

    private fun showDeleteZoneDialog(zone: SafeZone) {
        val ctx = requireContext()
        AlertDialog.Builder(ctx)
            .setTitle(R.string.ui_zone_delete_title)
            .setMessage(getString(R.string.ui_zone_delete_msg, zone.name))
            .setPositiveButton(R.string.ui_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val cfg = Graph.lockManager.current()
                    if (cfg?.active == true && cfg.unlockZoneId == zone.id) {
                        Toast.makeText(ctx, R.string.ui_zone_in_use, Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    Graph.db.safeZoneDao().delete(zone.id)
                    zones = Graph.db.safeZoneDao().getAll()
                    renderZones()
                    runCatching { Graph.geofences.sync(Graph.lockManager.current(), zones) }
                    Toast.makeText(ctx, R.string.ui_zone_deleted, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.ui_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        zoneCard = null
    }
}
