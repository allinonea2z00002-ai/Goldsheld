package com.goldsheld.app.ui

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.goldsheld.app.R

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

fun View.setBottomMargin(dp: Int) {
    val lp = layoutParams as ViewGroup.MarginLayoutParams
    lp.bottomMargin = context.dp(dp)
    layoutParams = lp
}

fun Context.color(res: Int): Int = ContextCompat.getColor(this, res)

/** Western digits (0-9) -> Bengali digits (০-৯). Other characters pass through unchanged. */
fun bnDigits(text: String): String =
    text.map { c -> if (c in '0'..'9') ('০' + (c - '0')) else c }.joinToString("")

fun Int.bn(): String = bnDigits(toString())
fun Long.bn(): String = bnDigits(toString())

/** "১৮ ঘণ্টা ৪০ মিনিট" / "৪৫ মিনিট" / "৩ ঘণ্টা" / "০ মিনিট" for a duration in ms. */
fun formatBnDuration(ms: Long): String {
    val totalMin = ms.coerceAtLeast(0) / 60_000
    val hours = totalMin / 60
    val minutes = totalMin % 60
    return when {
        totalMin <= 0 -> "০ মিনিট"
        hours == 0L -> "${minutes.bn()} মিনিট"
        minutes == 0L -> "${hours.bn()} ঘণ্টা"
        else -> "${hours.bn()} ঘণ্টা ${minutes.bn()} মিনিট"
    }
}

enum class ChipStyle { SELECTED, UNSELECTED, NEUTRAL, ACCENT, DASHED }

/** Pill text used for tags, time options and sub tabs. */
fun Context.pill(text: String, style: ChipStyle, minHeightDp: Int = 0): TextView {
    val tv = TextView(this)
    tv.text = text
    tv.textSize = 13f
    tv.gravity = android.view.Gravity.CENTER
    tv.minHeight = dp(minHeightDp)
    applyPill(tv, style)
    return tv
}

fun Context.applyPill(tv: TextView, style: ChipStyle) {
    when (style) {
        ChipStyle.SELECTED -> {
            tv.setBackgroundResource(R.drawable.bg_chip_selected)
            tv.setTextColor(color(R.color.accent))
            tv.setTypeface(null, android.graphics.Typeface.BOLD)
            tv.setPadding(dp(16), dp(6), dp(16), dp(6))
        }
        ChipStyle.UNSELECTED -> {
            tv.setBackgroundResource(R.drawable.bg_chip_unselected)
            tv.setTextColor(color(R.color.text))
            tv.setTypeface(null, android.graphics.Typeface.BOLD)
            tv.setPadding(dp(16), dp(6), dp(16), dp(6))
        }
        ChipStyle.NEUTRAL -> {
            tv.setBackgroundResource(R.drawable.bg_chip)
            tv.setTextColor(color(R.color.text))
            tv.setTypeface(null, android.graphics.Typeface.NORMAL)
            tv.setPadding(dp(12), dp(4), dp(12), dp(4))
            tv.textSize = 12f
        }
        ChipStyle.ACCENT -> {
            tv.setBackgroundResource(R.drawable.bg_chip_selected)
            tv.setTextColor(color(R.color.accent))
            tv.setTypeface(null, android.graphics.Typeface.NORMAL)
            tv.setPadding(dp(12), dp(4), dp(12), dp(4))
            tv.textSize = 12f
        }
        ChipStyle.DASHED -> {
            tv.setBackgroundResource(R.drawable.bg_chip_dashed)
            tv.setTextColor(color(R.color.muted))
            tv.setTypeface(null, android.graphics.Typeface.NORMAL)
            tv.setPadding(dp(12), dp(4), dp(12), dp(4))
        }
    }
}

fun View.tintBackground(colorRes: Int) {
    backgroundTintList = ColorStateList.valueOf(context.color(colorRes))
}
