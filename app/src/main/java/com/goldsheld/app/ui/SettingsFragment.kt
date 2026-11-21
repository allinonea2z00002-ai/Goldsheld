package com.goldsheld.app.ui

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.chip.ChipGroup
import com.goldsheld.app.R
import com.goldsheld.app.ThemePrefs
import com.goldsheld.app.databinding.ItemFeatureRowBinding
import com.goldsheld.app.databinding.ItemStatusRowBinding

/** Settings: theme switch (Dark / White / System) works; the other rows are placeholders. */
class SettingsFragment : CardsFragment() {
    override val titleRes = R.string.set_title
    override val subtitleRes = R.string.set_sub

    override fun buildList(list: LinearLayout) {
        val ctx = requireContext()
        label(list, R.string.set_theme)

        val modes = listOf(
            R.string.theme_system to AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            R.string.theme_light to AppCompatDelegate.MODE_NIGHT_NO,
            R.string.theme_dark to AppCompatDelegate.MODE_NIGHT_YES
        )
        val current = ThemePrefs.get(ctx)
        val group = ChipGroup(ctx)
        group.chipSpacingHorizontal = ctx.dp(8)
        modes.forEach { (res, mode) ->
            val pill = ctx.pill(getString(res), if (mode == current) ChipStyle.SELECTED else ChipStyle.UNSELECTED, 44)
            pill.isClickable = true
            pill.isFocusable = true
            pill.setOnClickListener { if (mode != ThemePrefs.get(ctx)) ThemePrefs.set(ctx, mode) }
            group.addView(pill)
        }
        val glp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        glp.topMargin = ctx.dp(8)
        glp.bottomMargin = ctx.dp(16)
        list.addView(group, glp)

        label(list, R.string.set_general)
        val card = LinearLayout(ctx)
        card.orientation = LinearLayout.VERTICAL
        card.setBackgroundResource(R.drawable.bg_card)
        card.setPadding(ctx.dp(14), ctx.dp(6), ctx.dp(14), ctx.dp(6))

        val notif = ItemFeatureRowBinding.inflate(layoutInflater, card, false)
        notif.label.setText(R.string.set_notifications)
        notif.sw.isChecked = true
        card.addView(notif.root)

        listOf(
            R.string.set_language to R.string.set_language_value,
            R.string.set_trusted to R.string.set_trusted_value,
            R.string.set_about to R.string.set_about_value
        ).forEach { (l, v) ->
            val row = ItemStatusRowBinding.inflate(layoutInflater, card, false)
            row.label.setText(l)
            row.state.setText(v)
            row.state.setTextColor(ctx.color(R.color.muted))
            card.addView(row.root)
        }
        val clp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        clp.topMargin = ctx.dp(8)
        list.addView(card, clp)
    }

    private fun label(list: LinearLayout, res: Int) {
        val tv = TextView(requireContext())
        tv.setText(res)
        tv.setTextColor(requireContext().color(R.color.muted))
        tv.textSize = 13f
        list.addView(tv)
    }
}
