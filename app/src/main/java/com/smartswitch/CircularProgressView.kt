package com.smartswitch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 25f
        color =  Color.parseColor("#D0E1FB")

    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND // This gives you the rounded end!
        strokeWidth = 25f
        color =  Color.parseColor("#1D3252")
    }

    private var progress = 0f // from 0f to 100f

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 100f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = 30f
        val size = min(width, height).toFloat()
        val radius = (size - padding * 2) / 2
        val centerX = width / 2f
        val centerY = height / 2f
        val rect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // Background ring
        canvas.drawArc(rect, 0f, 360f, false, backgroundPaint)

        // Progress arc (starts at top)
        val sweepAngle = (progress / 100f) * 360f
        canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)
    }
}
