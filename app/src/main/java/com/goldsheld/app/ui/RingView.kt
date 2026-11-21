package com.goldsheld.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.goldsheld.app.R

/** Circular progress ring used on Home (track + accent arc). */
class RingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var progress: Float = 0.62f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private val strokeWidthPx = 12f * resources.displayMetrics.density
    private val rect = RectF()
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        color = ContextCompat.getColor(context, R.color.ring_track)
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.accent)
    }

    override fun onDraw(canvas: Canvas) {
        val half = strokeWidthPx / 2f
        rect.set(half, half, width - half, height - half)
        canvas.drawArc(rect, 0f, 360f, false, track)
        canvas.drawArc(rect, -90f, 360f * progress, false, arc)
    }
}
