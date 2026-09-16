package com.fastbrowser.xp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

/** Draws a computer-style mouse pointer over the WebView. It never consumes touch events. */
class MousePointerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2.2f }
    private var px = 45f
    private var py = 45f
    private val path = Path()

    init { setLayerType(View.LAYER_TYPE_SOFTWARE, null) }

    fun moveBy(dx: Float, dy: Float) {
        px = min(max(3f, px + dx), max(3f, width - 6f))
        py = min(max(3f, py + dy), max(3f, height - 8f))
        invalidate()
    }

    fun xPos(): Float = px
    fun yPos(): Float = py

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        path.reset()
        path.moveTo(px, py)
        path.lineTo(px, py + 25f)
        path.lineTo(px + 7f, py + 19f)
        path.lineTo(px + 14f, py + 34f)
        path.lineTo(px + 18f, py + 32f)
        path.lineTo(px + 11f, py + 17f)
        path.lineTo(px + 22f, py + 17f)
        path.close()
        canvas.drawPath(path, fill)
        canvas.drawPath(path, stroke)
    }
}
