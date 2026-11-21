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
    val extraDashedTag: String? = null,
    /** User toggle of the switch. Not fired by programmatic isChecked changes. */
    val onCheckedChange: ((Boolean) -> Unit)? = null,
    /** Tap on a tag pill (receives the tag text). */
    val onTagClick: ((String) -> Unit)? = null,
    /** Tap on the dashed "+ add" tag. */
    val onAddTagClick: (() -> Unit)? = null
)

/**
 * Adds a card with an icon, title, switch, description, optional tags and a status note.
 * Returns the binding so the caller can update the switch or note later.
 */
fun LinearLayout.addSwitchCard(inflater: LayoutInflater, data: SwitchCardData): ItemSwitchCardBinding {
    val item = ItemSwitchCardBinding.inflate(inflater, this, false)
    item.icon.setImageResource(data.icon)
    item.title.text = data.title
    item.sw.isChecked = data.checked
    item.sw.contentDescription = data.title
    // Click, not OnCheckedChangeListener: programmatic isChecked changes — e.g. reverting a
    // toggle the lock guard refused — must not re-fire the callback.
    item.sw.setOnClickListener { data.onCheckedChange?.invoke(item.sw.isChecked) }
    if (data.desc.isEmpty()) item.desc.visibility = View.GONE else item.desc.text = data.desc
    if (data.tags.isNotEmpty() || data.extraDashedTag != null) {
        item.tags.visibility = View.VISIBLE
        data.tags.forEach { (text, style) ->
            val pill = context.pill(text, style)
            val onTagClick = data.onTagClick
            if (onTagClick != null) {
                pill.isClickable = true
                pill.isFocusable = true
                pill.setOnClickListener { onTagClick(text) }
            }
            item.tags.addView(pill)
        }
        data.extraDashedTag?.let {
            val pill = context.pill(it, ChipStyle.DASHED)
            val onAddTagClick = data.onAddTagClick
            if (onAddTagClick != null) {
                pill.isClickable = true
                pill.isFocusable = true
                pill.setOnClickListener { onAddTagClick() }
            }
            item.tags.addView(pill)
        }
    }
    if (data.note == null) {
        item.note.visibility = View.GONE
    } else {
        item.note.text = data.note
        item.note.setTextColor(context.color(if (data.noteOk) R.color.ok else R.color.warn_text))
    }
    item.root.setBottomMarginOf(10)
    addView(item.root)
    return item
}

private fun View.setBottomMarginOf(dp: Int) {
    layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).also { it.bottomMargin = context.dp(dp) }
}
