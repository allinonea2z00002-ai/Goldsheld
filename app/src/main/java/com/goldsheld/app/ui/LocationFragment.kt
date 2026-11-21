package com.goldsheld.app.ui

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.goldsheld.app.R
import com.goldsheld.app.databinding.ItemPlaceRowBinding

/** Location tab: saved zones and a history-clear action (UI only). */
class LocationFragment : CardsFragment() {
    override val titleRes = R.string.loc_title
    override val subtitleRes = R.string.loc_sub
    override fun bottomButtonText(): String = getString(R.string.loc_add)

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
        listOf("অফিস" to "ব্যাসার্ধ ২০০ মি · আসার সময় ৬ ঘণ্টার লক", "বাসা" to "ব্যাসার্ধ ১৫০ মি").forEach { (name, sub) ->
            val row = ItemPlaceRowBinding.inflate(layoutInflater, card, false)
            row.radio.setImageResource(R.drawable.ic_location)
            row.radio.setColorFilter(ctx.color(R.color.accent))
            row.name.text = name
            row.sub.text = sub
            card.addView(row.root)
        }
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = ctx.dp(8)
        lp.bottomMargin = ctx.dp(14)
        list.addView(card, lp)

        val clear = TextView(ctx)
        clear.setText(R.string.loc_clear)
        clear.setTextColor(ctx.color(R.color.danger))
        clear.textSize = 14f
        clear.setTypeface(null, android.graphics.Typeface.BOLD)
        clear.minHeight = ctx.dp(44)
        clear.gravity = android.view.Gravity.CENTER_VERTICAL
        clear.isClickable = true
        clear.isFocusable = true
        clear.setOnClickListener { ctx.demoToast() }
        list.addView(clear)
    }
}
