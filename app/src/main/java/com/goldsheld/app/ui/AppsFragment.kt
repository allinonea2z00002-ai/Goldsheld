package com.goldsheld.app.ui

import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.material.chip.ChipGroup
import com.goldsheld.app.R
import com.goldsheld.app.databinding.ItemAppCardBinding
import com.goldsheld.app.databinding.ItemFeatureRowBinding

/** Whole-app blocks and in-app feature blocks (Reels, search, Shorts). */
class AppsFragment : CardsFragment() {
    override val titleRes = R.string.apps_title
    override val subtitleRes = R.string.apps_sub
    override fun bottomButtonText(): String = getString(R.string.apps_add)

    override fun buildList(list: LinearLayout) {
        val ctx = requireContext()

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

        addApp(list, "In", "Instagram", "অ্যাপ চালু · ২টি ফিচার বন্ধ", true, 1, true,
            features = listOf("Reels" to true, "এক্সপ্লোর / সার্চ" to true, "ডিরেক্ট মেসেজ" to false))
        addApp(list, "Tg", "Telegram", "অ্যাপ চালু · সার্চ বন্ধ", true, 2, false,
            info = "সার্চ বাটন বা সার্চ বক্স খুললে Back চেপে ফেরত পাঠানো হবে।")
        addApp(list, "YT", "YouTube", "রাত ১০টা – সকাল ৬টা পুরো বন্ধ", false, 3, false,
            tags = listOf("Shorts বন্ধ", "দৈনিক ৩০ মিনিট"))
        addApp(list, "Fb", "Facebook", "লক চলাকালে পুরো বন্ধ", false, 4, false,
            tags = listOf("অফিসে থাকলে বন্ধ"))
    }

    private fun addApp(
        list: LinearLayout, badge: String, name: String, sub: String, subOk: Boolean, badgeIndex: Int,
        selected: Boolean, features: List<Pair<String, Boolean>> = emptyList(), info: String? = null,
        tags: List<String> = emptyList()
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
        features.forEach { (label, on) ->
            val row = ItemFeatureRowBinding.inflate(layoutInflater, item.features, false)
            row.label.text = label
            row.sw.isChecked = on
            row.sw.contentDescription = "$label ব্লক"
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
        item.root.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).also { it.topMargin = ctx.dp(10) }
        list.addView(item.root)
    }
}
