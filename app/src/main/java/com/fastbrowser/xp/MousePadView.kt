package com.fastbrowser.xp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/** Transparent, visible dashed mouse-pad area. Finger movement acts like a computer mouse. */
class MousePadView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    var onMoveDelta: ((Float, Float) -> Unit)? = null
    var onPadClick: (() -> Unit)? = null
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(150, 20, 65, 105)
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(7f, 6f), 0f)
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(145, 20, 65, 105)
        textSize = 12f
        textAlign = Paint.Align.CENTER
    }
    private var downX = 0f
    private var downY = 0f
    private var moved = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = 2f
        canvas.drawRect(inset, inset, width - inset, height - inset, border)
        canvas.drawText("MOUSE", width / 2f, height / 2f + 4f, label)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x; downY = event.y; moved = false
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (abs(dx) > 0.3f || abs(dy) > 0.3f) moved = true
                onMoveDelta?.invoke(dx * 1.8f, dy * 1.8f)
                downX = event.x; downY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!moved) onPadClick?.invoke()
                parent?.requestDisallowInterceptTouchEvent(false)
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick(); return true
    }
}
