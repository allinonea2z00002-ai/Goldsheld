package com.goldsheld.app.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.ChipGroup
import com.goldsheld.app.Graph
import com.goldsheld.app.R
import com.goldsheld.app.data.content.FeatureRulePack
import com.goldsheld.app.data.db.BlockedApplication
import com.goldsheld.app.data.prefs.ContentFilterStore
import com.goldsheld.app.databinding.ItemAppCardBinding
import com.goldsheld.app.databinding.ItemFeatureRowBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Whole-app blocks and in-app feature blocks (Reels, search, Shorts).
 *
 * How the feature switches map to the real engine:
 * - Instagram Reels / YouTube Shorts have a [ContentFilterStore] feed toggle (the reference
 *   Reels screen's pattern). It gates the fallback feed detection; the JSON rule pack
 *   enforces the same features whenever the feature-block module is on.
 * - Instagram Explore/Search, Telegram search and Facebook Reels exist only as rules in the
 *   pack: the engine enforces the pack as a whole, with no per-rule off switch. Their
 *   switches show the real state (rule present + module on) and explain this on tap.
 * - Instagram Direct Message has no rule at all: its switch stays off.
 *
 * The filter chips (সব অ্যাপ / ফিচার ব্লক / সময়-লোকেশন) are visual only — they do not
 * filter the list. Added apps default to block-during-lock; per-app schedule, location
 * and daily-limit editors are not exposed in this UI.
 */
class AppsFragment : CardsFragment() {
    override val titleRes = R.string.apps_title
    override val subtitleRes = R.string.apps_sub
    override fun bottomButtonText(): String = getString(R.string.apps_add)

    /** One feature row: its label, real on/off state, and what a user toggle does. */
    private data class FeatureToggle(
        val label: String,
        val checked: Boolean,
        val onToggle: ((Boolean, SwitchCompat) -> Unit)? = null
    )

    private var featureModuleOn: Boolean = true
    private var ruleIds: Set<String> = emptySet()
    private var addedContainer: LinearLayout? = null
    private var addedRules: List<BlockedApplication> = emptyList()

    override fun buildList(list: LinearLayout) {
        val ctx = requireContext()
        ruleIds = FeatureRulePack.load(ctx).map { it.id }.toSet()
        seedFeedDefaultsOnce()

        val filters = ChipGroup(ctx)
        filters.isSingleLine = false
        filters.chipSpacingHorizontal = ctx.dp(8)
        val names = listOf("সব অ্যাপ", "ফিচার ব্লক", "সময় / লোকেশন")
        val views = names.mapIndexed { i, n ->
            ctx.pill(n, if (i == 0) ChipStyle.SELECTED else ChipStyle.UNSELECTED, 44).also { filters.addView(it) }
        }
        views.forEachIndexed { i, v ->
            v.isClickable = true
            v.isFocusable = true
            v.setOnClickListener { views.forEachIndexed { j, o -> ctx.applyPill(o, if (j == i) ChipStyle.SELECTED else ChipStyle.UNSELECTED) } }
        }
        list.addView(filters)

        addApp(
            list, "In", "Instagram", "অ্যাপ চালু · ২টি ফিচার বন্ধ", true, 1, true,
            features = listOf(
                feedToggle("Reels", ContentFilterStore.FEED_INSTAGRAM_REELS),
                packToggle("এক্সপ্লোর / সার্চ", "instagram_explore_search"),
                noRuleToggle("ডিরেক্ট মেসেজ")
            )
        )
        addApp(
            list, "Tg", "Telegram", "অ্যাপ চালু · সার্চ বন্ধ", true, 2, false,
            features = listOf(packToggle("সার্চ", "telegram_search")),
            info = "সার্চ বাটন বা সার্চ বক্স খুললে Back চেপে ফেরত পাঠানো হবে।"
        )
        addApp(
            list, "YT", "YouTube", "রাত ১০টা – সকাল ৬টা পুরো বন্ধ", false, 3, false,
            features = listOf(feedToggle("Shorts", ContentFilterStore.FEED_YOUTUBE_SHORTS)),
            tags = listOf("Shorts বন্ধ", "দৈনিক ৩০ মিনিট")
        )
        addApp(
            list, "Fb", "Facebook", "লক চলাকালে পুরো বন্ধ", false, 4, false,
            features = listOf(packToggle("Reels", "facebook_reels")),
            tags = listOf("অফিসে থাকলে বন্ধ")
        )

        val added = LinearLayout(ctx)
        added.orientation = LinearLayout.VERTICAL
        addedContainer = added
        list.addView(added)
        renderAdded()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            featureModuleOn = Graph.settings.get().moduleFeatureBlock
            // Rebuild once with the real module state (the first build assumed it on).
            binding.list.removeAllViews()
            buildList(binding.list)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            Graph.db.blockedAppDao().observeAll().collect { rules ->
                addedRules = rules
                renderAdded()
            }
        }
    }

    // ------------------------------------------------------------ feature toggles

    /** A feed with a ContentFilterStore key: a real, persistent on/off toggle. */
    private fun feedToggle(label: String, feedKey: String): FeatureToggle {
        val ctx = requireContext()
        return FeatureToggle(label, ContentFilterStore.feedEnabled(ctx, feedKey)) { on, _ ->
            ContentFilterStore.setFeedEnabled(ctx, feedKey, on)
            // The fallback feed detection also needs the reels master on.
            if (on) ContentFilterStore.setReelsEnabled(ctx, true)
        }
    }

    /** A rule that only exists in the pack: no per-rule off switch exists in the engine. */
    private fun packToggle(label: String, ruleId: String): FeatureToggle {
        val on = ruleIds.contains(ruleId) && featureModuleOn
        return FeatureToggle(label, on) { _, sw ->
            sw.isChecked = on
            Toast.makeText(requireContext(), R.string.ui_feature_pack_note, Toast.LENGTH_LONG).show()
        }
    }

    /** A feature with no rule at all: always off, and tapping says so. */
    private fun noRuleToggle(label: String): FeatureToggle =
        FeatureToggle(label, false) { _, sw ->
            sw.isChecked = false
            Toast.makeText(requireContext(), R.string.ui_feature_no_rule, Toast.LENGTH_SHORT).show()
        }

    /** Reference S11 defaults, applied once: Shorts and Instagram Reels on. */
    private fun seedFeedDefaultsOnce() {
        val ctx = requireContext()
        val prefs = ctx.getSharedPreferences(SEED_PREFS, android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SEEDED, false)) return
        ContentFilterStore.setFeedEnabled(ctx, ContentFilterStore.FEED_YOUTUBE_SHORTS, true)
        ContentFilterStore.setFeedEnabled(ctx, ContentFilterStore.FEED_INSTAGRAM_REELS, true)
        ContentFilterStore.setReelsEnabled(ctx, true)
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }

    // ------------------------------------------------------------ added apps

    private fun renderAdded() {
        val container = addedContainer ?: return
        container.removeAllViews()
        addedRules.forEachIndexed { index, rule ->
            val badge = rule.label.take(2).uppercase()
            addApp(
                container, badge, rule.label, "লক চলাকালে পুরো বন্ধ", true,
                (index % 4) + 1, false,
                onLongPress = { showDeleteAppDialog(rule) }
            )
        }
    }

    private fun showDeleteAppDialog(rule: BlockedApplication) {
        val ctx = requireContext()
        AlertDialog.Builder(ctx)
            .setTitle(R.string.ui_app_delete_title)
            .setMessage(getString(R.string.ui_app_delete_msg, rule.label))
            .setPositiveButton(R.string.ui_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    Graph.db.blockedAppDao().delete(rule.packageName)
                    Toast.makeText(ctx, R.string.ui_app_deleted, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.ui_cancel, null)
            .show()
    }

    override fun onBottomButton() {
        viewLifecycleOwner.lifecycleScope.launch {
            val ctx = requireContext()
            val apps = withContext(Dispatchers.IO) {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                ctx.packageManager.queryIntentActivities(intent, 0)
                    .mapNotNull { resolve ->
                        resolve.activityInfo?.let { info ->
                            info.loadLabel(ctx.packageManager).toString() to info.packageName
                        }
                    }
                    .filter { it.second != ctx.packageName }
                    .distinctBy { it.second }
                    .sortedBy { it.first.lowercase() }
            }
            if (apps.isEmpty()) {
                Toast.makeText(ctx, R.string.ui_no_apps, Toast.LENGTH_SHORT).show()
                return@launch
            }
            AlertDialog.Builder(ctx)
                .setTitle(R.string.ui_add_app_title)
                .setItems(apps.map { it.first }.toTypedArray()) { _, which ->
                    val (label, pkg) = apps[which]
                    viewLifecycleOwner.lifecycleScope.launch {
                        if (Graph.db.blockedAppDao().get(pkg) != null) {
                            Toast.makeText(ctx, R.string.ui_app_already_added, Toast.LENGTH_SHORT).show()
                        } else {
                            Graph.db.blockedAppDao().upsert(
                                BlockedApplication(packageName = pkg, label = label, blockDuringLock = true)
                            )
                            Toast.makeText(ctx, R.string.ui_app_added, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton(R.string.ui_cancel, null)
                .show()
        }
    }

    // ------------------------------------------------------------ card builder

    private fun addApp(
        list: LinearLayout, badge: String, name: String, sub: String, subOk: Boolean, badgeIndex: Int,
        selected: Boolean, features: List<FeatureToggle> = emptyList(), info: String? = null,
        tags: List<String> = emptyList(), onLongPress: (() -> Unit)? = null
    ) {
        val ctx = requireContext()
        val item = ItemAppCardBinding.inflate(layoutInflater, list, false)
        item.root.setBackgroundResource(if (selected) R.drawable.bg_card_selected else R.drawable.bg_card)
        val bgRes = when (badgeIndex) { 1 -> R.color.badge1_bg; 2 -> R.color.badge2_bg; 3 -> R.color.badge3_bg; else -> R.color.badge4_bg }
        val fgRes = when (badgeIndex) { 1 -> R.color.badge1_fg; 2 -> R.color.badge2_fg; 3 -> R.color.badge3_fg; else -> R.color.badge4_fg }
        item.badge.text = badge
        item.badge.backgroundTintList = ColorStateList.valueOf(ctx.color(bgRes))
        item.badge.setTextColor(ctx.color(fgRes))
        item.name.text = name
        item.sub.text = sub
        item.sub.setTextColor(ctx.color(if (subOk) R.color.ok else R.color.warn_text))
        features.forEach { toggle ->
            val row = ItemFeatureRowBinding.inflate(layoutInflater, item.features, false)
            row.label.text = toggle.label
            row.sw.isChecked = toggle.checked
            row.sw.contentDescription = "${toggle.label} ব্লক"
            // Click, not OnCheckedChangeListener: reverting a switch must not re-fire.
            row.sw.setOnClickListener { toggle.onToggle?.invoke(row.sw.isChecked, row.sw) }
            item.features.addView(row.root)
        }
        if (info != null) {
            item.info.visibility = View.VISIBLE
            item.info.text = info
        }
        if (tags.isNotEmpty()) {
            item.tags.visibility = View.VISIBLE
            tags.forEach { item.tags.addView(ctx.pill(it, ChipStyle.NEUTRAL)) }
        }
        if (onLongPress != null) {
            item.root.setOnLongClickListener {
                onLongPress()
                true
            }
        }
        item.root.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).also { it.topMargin = ctx.dp(10) }
        list.addView(item.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        addedContainer = null
    }

    private companion object {
        const val SEED_PREFS = "goldsheld_content_filters_seed"
        const val KEY_SEEDED = "feed_defaults_seeded"
    }
}
