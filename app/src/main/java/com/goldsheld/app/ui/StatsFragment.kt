package com.goldsheld.app.ui

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.goldsheld.app.R
import com.goldsheld.app.databinding.ItemStatCardBinding

/** Statistics screen (sample numbers in this UI-only project). */
class StatsFragment : CardsFragment() {
    override val titleRes = R.string.stats_title
    override val subtitleRes = R.string.stats_sub

    override fun buildList(list: LinearLayout) {
        val ctx = requireContext()
        val cards = listOf(
            R.string.stat_total to "১৮ ঘণ্টা ৪০ মিনিট",
            R.string.stat_streak to "৩ দিন",
            R.string.stat_blocks to "১২"
        )
        cards.forEach { (label, value) ->
            val item = ItemStatCardBinding.inflate(layoutInflater, list, false)
            item.label.setText(label)
            item.value.text = value
            item.root.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = ctx.dp(10) }
            list.addView(item.root)
        }

        val heading = TextView(ctx)
        heading.setText(R.string.stat_week)
        heading.setTextColor(ctx.color(R.color.muted))
        heading.textSize = 13f
        list.addView(heading)

        val chart = BarChartView(ctx)
        chart.values = listOf(2f, 3.5f, 1f, 4f, 6f, 5f, 2.5f)
        chart.labels = listOf("শনি", "রবি", "সোম", "মঙ্গল", "বুধ", "বৃহ", "শুক্র")
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = ctx.dp(10)
        list.addView(chart, lp)
    }
}
