package com.goldsheld.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.goldsheld.app.R

/** Seven simple bars with labels (sample data in this UI-only project). */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var values: List<Float> = emptyList()
        set(value) {
            field = value
            invalidate()
        }
    var labels: List<String> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.accent)
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.ring_track)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.muted)
        textSize = 11f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = (150 * density).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize(h, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        if (values.isEmpty()) return
        val max = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val labelH = 22f * density
        val chartH = height - labelH
        val slot = width.toFloat() / values.size
        val barW = slot * 0.42f
        val radius = barW / 2f
        for (i in values.indices) {
            val cx = slot * i + slot / 2f
            rect.set(cx - barW / 2f, 0f, cx + barW / 2f, chartH)
            canvas.drawRoundRect(rect, radius, radius, trackPaint)
            val barH = (values[i] / max * chartH).coerceAtLeast(barW)
            rect.set(cx - barW / 2f, chartH - barH, cx + barW / 2f, chartH)
            canvas.drawRoundRect(rect, radius, radius, barPaint)
            val label = labels.getOrNull(i).orEmpty()
            canvas.drawText(label, cx, height - 4f * density, textPaint)
        }
    }
}
