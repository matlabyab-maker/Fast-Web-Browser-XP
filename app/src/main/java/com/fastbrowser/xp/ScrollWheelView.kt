package com.fastbrowser.xp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import kotlin.math.abs

/** A small mouse-wheel-like control for smooth manual WebView scrolling. */
class ScrollWheelView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    var onScrollDelta: ((Int) -> Unit)? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lastY = 0f
    private var dragging = false
    private var velocityTracker: VelocityTracker? = null

    init {
        isClickable = true
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = (minOf(width, height) / 2f - 4f).coerceAtLeast(10f)

        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.rgb(236, 233, 216)
        canvas.drawCircle(cx, cy, r, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = android.graphics.Color.rgb(20, 65, 105)
        canvas.drawCircle(cx, cy, r, paint)

        // Mouse-wheel ridges
        paint.strokeWidth = 2f
        paint.color = android.graphics.Color.rgb(58, 110, 165)
        for (i in -2..2) {
            val y = cy + i * r * 0.23f
            canvas.drawLine(cx - r * 0.34f, y, cx + r * 0.34f, y, paint)
        }
        paint.style = Paint.Style.FILL
        paint.textSize = r * 0.36f
        paint.color = android.graphics.Color.rgb(20, 65, 105)
        canvas.drawText("↕", cx - paint.measureText("↕") / 2f, cy + r * 0.14f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastY = event.y
                dragging = true
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                val dy = event.y - lastY
                lastY = event.y
                if (abs(dy) > 0.2f) onScrollDelta?.invoke((dy * 4.0f).toInt())
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.addMovement(event)
                velocityTracker?.computeCurrentVelocity(1000)
                val vy = velocityTracker?.yVelocity ?: 0f
                // Small momentum, like a real wheel, without making the page jump.
                if (event.actionMasked == MotionEvent.ACTION_UP && abs(vy) > 700f) {
                    val extra = (vy * 0.08f).toInt().coerceIn(-700, 700)
                    onScrollDelta?.invoke(extra)
                }
                velocityTracker?.recycle()
                velocityTracker = null
                dragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
                performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
