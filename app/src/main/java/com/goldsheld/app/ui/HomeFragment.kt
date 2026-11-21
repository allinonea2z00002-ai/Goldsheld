package com.goldsheld.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.goldsheld.app.Graph
import com.goldsheld.app.R
import com.goldsheld.app.admin.DevicePolicyHelper
import com.goldsheld.app.data.prefs.ContentFilterStore
import com.goldsheld.app.databinding.FragmentHomeBinding
import com.goldsheld.app.databinding.ItemStatusRowBinding
import com.goldsheld.app.domain.CombineRule
import com.goldsheld.app.domain.LocationKind
import com.goldsheld.app.domain.LockEvaluator
import com.goldsheld.app.domain.LockMode
import com.goldsheld.app.domain.LockSnapshot
import com.goldsheld.app.domain.ModuleSnapshot
import com.goldsheld.app.security.Issue
import com.goldsheld.app.service.vpn.DnsVpnController
import com.goldsheld.app.util.DurationFormat
import com.goldsheld.app.util.Motivation
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Home: the real lock state, ticking every second. The ring, clock and percent come from the
 * lock snapshot; the status rows from the effective module snapshot + integrity state; the
 * warning banner only shows when a real integrity issue exists.
 *
 * The banner, the "লক চালু" pill and the unlock-conditions card have no ids in the layout,
 * so they are found once by walking the view tree (see [findUntaggedViews]). Nothing in the
 * layout file was changed; if a view cannot be found its update is skipped.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private data class Row(val label: String, val state: String, val ok: Boolean)

    // Views without ids in fragment_home.xml, found by their text.
    private var bannerView: View? = null
    private var bannerTitle: TextView? = null
    private var bannerBody: TextView? = null
    private var bannerAction: TextView? = null
    private var lockPill: TextView? = null
    private var unlockCard: View? = null
    private var rulePill: TextView? = null
    private var timeRow: View? = null
    private var timeValue: TextView? = null
    private var placeRow: View? = null
    private var placeLabel: TextView? = null
    private var placeValue: TextView? = null

    private var lastRowSignature: String? = null
    private var lastZoneId: Long = -1
    private var lastZoneName: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        findUntaggedViews()
        // Neutral until the first tick decides: never show the layout's stale defaults.
        binding.ring.progress = 0f
        binding.clock.text = "০০:০০"
        binding.percent.text = "লক চালু নেই"
        bannerView?.visibility = View.GONE
        unlockCard?.visibility = View.GONE

        binding.btnEmergency.setOnClickListener {
            // Reference emergency pattern (BlockActivity): open the dialer.
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:")))
        }
        binding.btnUrge.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.btn_urge)
                .setMessage(Motivation.random(requireContext()))
                .setPositiveButton(R.string.ui_ok, null)
                .show()
        }
        // No layout change is allowed for the override, so it lives on a long-press of the ring.
        binding.ring.setOnLongClickListener {
            onRingLongPress()
            true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { refreshSlow() }
            var tick = 0
            while (isActive) {
                runCatching { refreshFast() }
                tick++
                if (tick % 15 == 0) runCatching { refreshSlow() }
                delay(1000)
            }
        }
    }

    // ------------------------------------------------------------ fast tick

    /** Ring, clock, percent and unlock card from the live lock snapshot (read-only, no writes). */
    private suspend fun refreshFast() {
        if (!isAdded) return
        val cfg = Graph.lockManager.current()
        val snapshot = LockEvaluator.snapshot(
            cfg,
            SystemClock.elapsedRealtime(),
            System.currentTimeMillis(),
            Graph.lockManager.bootCount()
        )
        if (!snapshot.active) {
            lockPill?.text = "লক বন্ধ"
            binding.ring.progress = 0f
            binding.clock.text = "০০:০০"
            binding.percent.text = "লক চালু নেই"
            unlockCard?.visibility = View.GONE
            return
        }
        lockPill?.text = getString(R.string.lock_on)
        binding.ring.progress = snapshot.progress
        if (snapshot.mode.usesTime) {
            binding.clock.text = DurationFormat.clock(snapshot.remainingMs)
            binding.percent.text = "${(snapshot.progress * 100).toInt().bn()}% সম্পন্ন"
        } else {
            binding.clock.text = zoneNameFor(snapshot.unlockZoneId) ?: "লোকেশন লক"
            binding.percent.text = if (snapshot.distanceSatisfied) "পৌঁছানো হয়েছে" else "পৌঁছালে খুলবে"
        }
        updateUnlockCard(snapshot)
    }

    private suspend fun zoneNameFor(zoneId: Long): String? {
        if (zoneId <= 0) return null
        if (zoneId != lastZoneId) {
            lastZoneName = Graph.db.safeZoneDao().get(zoneId)?.name
            lastZoneId = zoneId
        }
        return lastZoneName
    }

    private suspend fun updateUnlockCard(snapshot: LockSnapshot) {
        val card = unlockCard ?: return
        card.visibility = View.VISIBLE
        if (snapshot.mode == LockMode.COMBINED) {
            rulePill?.visibility = View.VISIBLE
            rulePill?.text = getString(
                if (snapshot.rule == CombineRule.ANY) R.string.rule_any else R.string.all_title
            )
        } else {
            rulePill?.visibility = View.GONE
        }
        if (snapshot.mode.usesTime) {
            timeRow?.visibility = View.VISIBLE
            timeValue?.text = if (snapshot.timeSatisfied) "সময় শেষ"
            else "${formatBnDuration(snapshot.remainingMs)} বাকি"
        } else {
            timeRow?.visibility = View.GONE
        }
        if (snapshot.mode.usesDistance) {
            placeRow?.visibility = View.VISIBLE
            if (snapshot.locationKind == LocationKind.ZONE) {
                placeLabel?.text = "${zoneNameFor(snapshot.unlockZoneId) ?: "জায়গা"}-এ পৌঁছালে"
                placeValue?.text = if (snapshot.distanceSatisfied) "পৌঁছানো হয়েছে" else "এখনও পৌঁছানো যায়নি"
            } else {
                placeLabel?.text = "দূরত্ব পার হলে"
                placeValue?.text = if (snapshot.distanceSatisfied) "লক্ষ্য পার হয়েছে"
                else "${DurationFormat.distance(snapshot.distanceM)} / ${DurationFormat.distance(snapshot.distanceLimitM.toFloat())}"
            }
        } else {
            placeRow?.visibility = View.GONE
        }
    }

    // ------------------------------------------------------------ slow tick

    /** Status rows + warning banner from the real module and integrity state (every 15 s). */
    private suspend fun refreshSlow() {
        if (!isAdded) return
        val ctx = requireContext()
        val cfg = Graph.lockManager.current()
        val settings = Graph.settings.get()
        // During a lock the snapshot decides what is on; outside a lock the live settings do.
        val modules = if (cfg?.active == true) ModuleSnapshot.fromLock(cfg)
        else ModuleSnapshot.fromSettings(settings)
        val a11yOn = Graph.integrity.isAccessibilityEnabled()
        val vpnOn = DnsVpnController.isRunning(ctx)
        val deviceOwner = DevicePolicyHelper.isDeviceOwner(ctx)
        val blockedCount = Graph.db.blockedAppDao().getAll().size
        val keywordsOn = ContentFilterStore.keywordsEnabled(ctx)

        val rows = listOf(
            Row(
                "পাওয়ার মেনু ব্লক",
                when {
                    !modules.powerMenu -> "বন্ধ"
                    deviceOwner -> "চালু"
                    else -> "আংশিক"
                },
                modules.powerMenu
            ),
            Row(
                "অ্যাপ ব্লক (${blockedCount.bn()}টি)",
                when {
                    !modules.appBlock -> "বন্ধ"
                    !a11yOn -> "বন্ধ — অনুমতি নেই"
                    else -> "চালু"
                },
                modules.appBlock && a11yOn
            ),
            Row(
                "অ্যাপের ফিচার ব্লক",
                when {
                    !modules.featureBlock -> "বন্ধ"
                    !a11yOn -> "বন্ধ — অনুমতি নেই"
                    else -> "চালু"
                },
                modules.featureBlock && a11yOn
            ),
            Row(
                "সাইট ব্লক (DNS)",
                when {
                    !modules.siteBlock -> "বন্ধ"
                    !vpnOn -> "বন্ধ — VPN বন্ধ"
                    else -> "চালু"
                },
                modules.siteBlock && vpnOn
            ),
            Row(
                "কীওয়ার্ড ব্লক",
                when {
                    !modules.keywordBlock || !keywordsOn -> "বন্ধ"
                    !a11yOn -> "বন্ধ — অনুমতি নেই"
                    else -> "চালু"
                },
                modules.keywordBlock && keywordsOn && a11yOn
            ),
            Row(
                "অ্যাপ ইনস্টল / আনইনস্টল ব্লক",
                when {
                    !modules.apkInstall && !modules.uninstall -> "বন্ধ"
                    deviceOwner -> "চালু"
                    !a11yOn -> "আংশিক — অনুমতি নেই"
                    else -> "আংশিক"
                },
                (modules.apkInstall || modules.uninstall) && (deviceOwner || a11yOn)
            )
        )
        val signature = rows.joinToString("|") { "${it.label}=${it.state}" }
        if (signature != lastRowSignature) {
            lastRowSignature = signature
            renderRows(rows)
        }

        val report = Graph.integrity.check()
        val critical = report.criticalIssues
        if (critical.isEmpty()) {
            bannerView?.visibility = View.GONE
        } else {
            bannerView?.visibility = View.VISIBLE
            val first = critical.first()
            if (first == Issue.ACCESSIBILITY_OFF) {
                bannerTitle?.setText(R.string.warn_a11y_title)
                bannerBody?.setText(R.string.warn_a11y_body)
                bannerAction?.setText(R.string.warn_a11y_action)
                bannerAction?.setOnClickListener {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            } else {
                bannerTitle?.setText(R.string.notif_integrity_title)
                bannerBody?.text = Graph.integrity.issueText(first)
                bannerAction?.text = "সুরক্ষা দেখুন →"
                bannerAction?.setOnClickListener {
                    (activity as? MainActivity)?.switchTab(R.id.nav_protect)
                }
            }
        }
    }

    private fun renderRows(rows: List<Row>) {
        val ctx = requireContext()
        binding.statusList.removeAllViews()
        rows.forEachIndexed { index, row ->
            val item = ItemStatusRowBinding.inflate(layoutInflater, binding.statusList, false)
            item.label.text = row.label
            item.state.text = row.state
            item.state.setTextColor(ctx.color(if (row.ok) R.color.ok else R.color.warn_text))
            binding.statusList.addView(item.root)
            if (index < rows.lastIndex) {
                val line = View(ctx)
                line.setBackgroundColor(ctx.color(R.color.divider))
                binding.statusList.addView(
                    line,
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(1))
                )
            }
        }
    }

    // ------------------------------------------------------- emergency override

    private fun onRingLongPress() {
        viewLifecycleOwner.lifecycleScope.launch {
            val ctx = requireContext()
            if (!Graph.lockManager.isActive()) {
                Toast.makeText(ctx, R.string.ui_no_lock, Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (!Graph.secure.hasOverrideWord()) {
                Toast.makeText(ctx, R.string.ui_no_override_word, Toast.LENGTH_LONG).show()
                return@launch
            }
            val input = EditText(ctx).apply {
                hint = getString(R.string.ui_override_hint)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                setPadding(ctx.dp(16), ctx.dp(12), ctx.dp(16), ctx.dp(12))
            }
            AlertDialog.Builder(ctx)
                .setTitle(R.string.ui_override_title)
                .setMessage(R.string.ui_override_msg)
                .setView(input)
                .setPositiveButton(R.string.ui_override_break) { _, _ ->
                    val word = input.text.toString()
                    viewLifecycleOwner.lifecycleScope.launch {
                        if (Graph.secure.overrideWordMatches(word)) {
                            // Always logged by the engine (EMERGENCY_OVERRIDE).
                            Graph.lockManager.emergencyOverride()
                            Toast.makeText(ctx, R.string.ui_override_done, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(ctx, R.string.ui_override_wrong, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton(R.string.ui_cancel, null)
                .show()
        }
    }

    // ------------------------------------------------------- untagged views

    private fun findTextView(root: View, text: String): TextView? {
        if (root is TextView && root.text.toString() == text) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findTextView(root.getChildAt(i), text)?.let { return it }
            }
        }
        return null
    }

    /** Finds the views the layout never gave an id, by their text. Missing views stay null. */
    private fun findUntaggedViews() {
        val root = binding.root
        lockPill = findTextView(root, getString(R.string.lock_on))
        bannerTitle = findTextView(root, getString(R.string.warn_a11y_title))
        bannerBody = findTextView(root, getString(R.string.warn_a11y_body))
        bannerAction = findTextView(root, getString(R.string.warn_a11y_action))
        bannerView = (bannerTitle?.parent as? View)?.parent as? View
        val unlockLabel = findTextView(root, getString(R.string.unlock_when))
        unlockCard = (unlockLabel?.parent as? View)?.parent as? View
        rulePill = findTextView(root, getString(R.string.rule_any))
        val timeLabel = findTextView(root, "সময় শেষ হলে")
        timeRow = timeLabel?.parent as? View
        timeValue = (timeRow as? ViewGroup)
            ?.let { row -> (0 until row.childCount).map { row.getChildAt(it) } }
            ?.filterIsInstance<TextView>()
            ?.getOrNull(1)
        val placeLabelView = findTextView(root, "অফিসে পৌঁছালে")
        placeRow = placeLabelView?.parent as? View
        placeLabel = placeLabelView
        placeValue = (placeRow as? ViewGroup)
            ?.let { row -> (0 until row.childCount).map { row.getChildAt(it) } }
            ?.filterIsInstance<TextView>()
            ?.getOrNull(1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bannerView = null
        bannerTitle = null
        bannerBody = null
        bannerAction = null
        lockPill = null
        unlockCard = null
        rulePill = null
        timeRow = null
        timeValue = null
        placeRow = null
        placeLabel = null
        placeValue = null
        _binding = null
    }
}
