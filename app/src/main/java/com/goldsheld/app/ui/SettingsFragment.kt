package com.goldsheld.app.ui

import android.content.Intent
import android.provider.Settings
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.chip.ChipGroup
import com.goldsheld.app.Graph
import com.goldsheld.app.R
import com.goldsheld.app.ThemePrefs
import com.goldsheld.app.databinding.ItemFeatureRowBinding
import com.goldsheld.app.databinding.ItemStatusRowBinding

/**
 * Settings: the theme switch works as before. The notifications row mirrors the system
 * notification setting (the ported SettingsStore has no notification preference — only
 * interruption alerts); tapping it opens the system app-notification settings. The
 * language, trusted-contact and about rows are display-only, unchanged. One programmatic
 * row is added for the emergency override word (stored encrypted via SecureStore).
 */
class SettingsFragment : CardsFragment() {
    override val titleRes = R.string.set_title
    override val subtitleRes = R.string.set_sub

    private var notifSwitch: SwitchCompat? = null
    private var overrideState: TextView? = null

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
        notif.sw.isChecked = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        notif.sw.setOnClickListener {
            // The switch mirrors the system setting; tapping opens the system screen.
            notif.sw.isChecked = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            startActivity(intent)
        }
        notifSwitch = notif.sw
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

        val override = ItemStatusRowBinding.inflate(layoutInflater, card, false)
        override.label.text = "জরুরি ওভাররাইড শব্দ"
        override.state.text = if (Graph.secure.hasOverrideWord()) "সেট করা আছে" else "সেট করা নেই"
        override.state.setTextColor(ctx.color(R.color.muted))
        override.root.isClickable = true
        override.root.isFocusable = true
        override.root.setOnClickListener { showOverrideWordDialog() }
        overrideState = override.state
        card.addView(override.root)

        val clp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        clp.topMargin = ctx.dp(8)
        list.addView(card, clp)
    }

    override fun onResume() {
        super.onResume()
        notifSwitch?.isChecked =
            NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
        overrideState?.text = if (Graph.secure.hasOverrideWord()) "সেট করা আছে" else "সেট করা নেই"
    }

    private fun showOverrideWordDialog() {
        val ctx = requireContext()
        val input = EditText(ctx).apply {
            hint = getString(R.string.ui_override_word_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(ctx.dp(16), ctx.dp(12), ctx.dp(16), ctx.dp(12))
        }
        AlertDialog.Builder(ctx)
            .setTitle(R.string.ui_override_word_title)
            .setMessage(R.string.ui_override_word_msg)
            .setView(input)
            .setPositiveButton(R.string.ui_save) { _, _ ->
                val word = input.text.toString().trim()
                if (word.isEmpty()) {
                    Toast.makeText(ctx, R.string.ui_override_word_empty, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                Graph.secure.setOverrideWord(word)
                overrideState?.text = "সেট করা আছে"
                Toast.makeText(ctx, R.string.ui_override_word_saved, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.ui_cancel, null)
            .show()
    }

    private fun label(list: LinearLayout, res: Int) {
        val tv = TextView(requireContext())
        tv.setText(res)
        tv.setTextColor(requireContext().color(R.color.muted))
        tv.textSize = 13f
        list.addView(tv)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        notifSwitch = null
        overrideState = null
    }
}
