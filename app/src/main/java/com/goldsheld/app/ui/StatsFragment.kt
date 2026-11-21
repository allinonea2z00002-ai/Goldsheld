package com.goldsheld.app.ui

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.goldsheld.app.Graph
import com.goldsheld.app.R
import com.goldsheld.app.data.StatsCalculator
import com.goldsheld.app.databinding.ItemStatCardBinding
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Statistics screen: real aggregates from the per-day statistics table — total protected
 * time, streak days, blocked attempts (website + app blocks) — and a real 7-day series
 * for the chart. Day labels are the same Bengali weekday names, computed from the
 * actual dates so each bar lines up with its day.
 */
class StatsFragment : CardsFragment() {
    override val titleRes = R.string.stats_title
    override val subtitleRes = R.string.stats_sub

    private var totalValue: TextView? = null
    private var streakValue: TextView? = null
    private var blocksValue: TextView? = null
    private var chart: BarChartView? = null

    override fun buildList(list: LinearLayout) {
        totalValue = addStatCard(list, R.string.stat_total, "০ মিনিট")
        streakValue = addStatCard(list, R.string.stat_streak, "০ দিন")
        blocksValue = addStatCard(list, R.string.stat_blocks, "০")

        val ctx = requireContext()
        val heading = TextView(ctx)
        heading.setText(R.string.stat_week)
        heading.setTextColor(ctx.color(R.color.muted))
        heading.textSize = 13f
        list.addView(heading)

        val chartView = BarChartView(ctx)
        chartView.values = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)
        chartView.labels = weekdayLabels(LocalDate.now())
        chart = chartView
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = ctx.dp(10)
        list.addView(chartView, lp)
    }

    private fun addStatCard(list: LinearLayout, labelRes: Int, initial: String): TextView {
        val item = ItemStatCardBinding.inflate(layoutInflater, list, false)
        item.label.setText(labelRes)
        item.value.text = initial
        item.root.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).also { it.bottomMargin = requireContext().dp(10) }
        list.addView(item.root)
        return item.value
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
    }

    private fun refreshStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            val rows = Graph.db.statsDao().getAll()
            val summary = StatsCalculator.summarize(rows, LocalDate.now())
            totalValue?.text = formatBnDuration(summary.totalMs)
            streakValue?.text = "${summary.streakDays.bn()} দিন"
            blocksValue?.text = (summary.websiteBlocks + summary.appBlocks).bn()
            chart?.values = summary.last7Days.map { (_, ms) -> ms / 3_600_000f }
            chart?.labels = summary.last7Days.map { (date, _) -> weekdayName(date) }
        }
    }

    private fun weekdayName(date: LocalDate): String = when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> "শনি"
        DayOfWeek.SUNDAY -> "রবি"
        DayOfWeek.MONDAY -> "সোম"
        DayOfWeek.TUESDAY -> "মঙ্গল"
        DayOfWeek.WEDNESDAY -> "বুধ"
        DayOfWeek.THURSDAY -> "বৃহ"
        else -> "শুক্র"
    }

    private fun weekdayLabels(today: LocalDate): List<String> =
        (6 downTo 0).map { weekdayName(today.minusDays(it.toLong())) }

    override fun onDestroyView() {
        super.onDestroyView()
        totalValue = null
        streakValue = null
        blocksValue = null
        chart = null
    }
}
