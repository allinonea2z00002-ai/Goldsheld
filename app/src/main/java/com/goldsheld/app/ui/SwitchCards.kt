package com.goldsheld.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.goldsheld.app.R
import com.goldsheld.app.databinding.ItemSwitchCardBinding

data class SwitchCardData(
    val icon: Int,
    val title: String,
    val desc: String,
    val checked: Boolean,
    val note: String? = null,
    val noteOk: Boolean = true,
    val tags: List<Pair<String, ChipStyle>> = emptyList(),
    val extraDashedTag: String? = null
)

/** Adds a card with an icon, title, switch, description, optional tags and a status note. */
fun LinearLayout.addSwitchCard(inflater: LayoutInflater, data: SwitchCardData) {
    val item = ItemSwitchCardBinding.inflate(inflater, this, false)
    item.icon.setImageResource(data.icon)
    item.title.text = data.title
    item.sw.isChecked = data.checked
    item.sw.contentDescription = data.title
    item.sw.setOnCheckedChangeListener { _, _ -> /* UI only */ }
    if (data.desc.isEmpty()) item.desc.visibility = View.GONE else item.desc.text = data.desc
    if (data.tags.isNotEmpty() || data.extraDashedTag != null) {
        item.tags.visibility = View.VISIBLE
        data.tags.forEach { (text, style) -> item.tags.addView(context.pill(text, style)) }
        data.extraDashedTag?.let { item.tags.addView(context.pill(it, ChipStyle.DASHED)) }
    }
    if (data.note == null) {
        item.note.visibility = View.GONE
    } else {
        item.note.text = data.note
        item.note.setTextColor(context.color(if (data.noteOk) R.color.ok else R.color.warn_text))
    }
    item.root.setBottomMarginOf(10)
    addView(item.root)
}

private fun View.setBottomMarginOf(dp: Int) {
    layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).also { it.bottomMargin = context.dp(dp) }
}
