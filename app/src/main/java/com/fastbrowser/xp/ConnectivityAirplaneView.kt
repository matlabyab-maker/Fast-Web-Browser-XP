package com.fastbrowser.xp

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.animation.ValueAnimator
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import kotlin.math.sin

/** Small XP-style status illustration: offline = plane in hangar, online = takeoff. */
class ConnectivityAirplaneView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
    private var online = false
    private var progress = 0f
    private var animator: ValueAnimator? = null
    private var cm: ConnectivityManager? = null
    private var callback: ConnectivityManager.NetworkCallback? = null

    init {
        setBackgroundColor(Color.TRANSPARENT)
        isClickable = false
        isFocusable = false
        cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        refreshConnectivity()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = refreshConnectivity()
                override fun onLost(network: Network) = refreshConnectivity()
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = refreshConnectivity()
            }
            try { cm?.registerDefaultNetworkCallback(callback!!) } catch (_: Exception) {}
        }
        postDelayed(object : Runnable {
            override fun run() {
                refreshConnectivity()
                postDelayed(this, 2500)
            }
        }, 2500)
    }

    private fun refreshConnectivity() {
        val manager = cm ?: return
        val active = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) manager.activeNetwork else null
        val caps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && active != null) manager.getNetworkCapabilities(active) else null
        val now = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        } else {
            @Suppress("DEPRECATION") val info = manager.activeNetworkInfo
            @Suppress("DEPRECATION") (info?.isConnected == true)
        }
        if (now != online) {
            online = now
            if (online) startTakeoff() else stopTakeoff()
            postInvalidateOnAnimation()
        }
    }

    private fun startTakeoff() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3200L
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { progress = it.animatedValue as Float; postInvalidateOnAnimation() }
            start()
        }
    }

    private fun stopTakeoff() {
        animator?.cancel()
        animator = null
        progress = 0f
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try { callback?.let { cm?.unregisterNetworkCallback(it) } } catch (_: Exception) {}
        }
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        if (online) drawTakeoff(canvas, w, h) else drawHangar(canvas, w, h)
    }

    private fun drawHangar(c: Canvas, w: Float, h: Float) {
        val base = h - 7f
        paint.color = Color.argb(205, 210, 220, 230)
        c.drawRoundRect(8f, 7f, w - 8f, h - 2f, 7f, 7f, paint)
        paint.color = Color.rgb(80, 110, 140)
        c.drawRect(12f, 18f, w - 12f, base, paint)
        paint.color = Color.rgb(160, 180, 195)
        c.drawRect(17f, 23f, w - 17f, base, paint)
        paint.color = Color.rgb(48, 72, 92)
        c.drawRect(21f, 28f, w - 21f, base, paint)
        // plane nose sticking out of the hangar
        drawPlane(c, w * 0.54f, h * 0.60f, 0f, 1.0f, Color.WHITE)
        paint.color = Color.rgb(255, 170, 0)
        c.drawCircle(w * 0.77f, h * 0.60f, 2.2f, paint)
        stroke.color = Color.argb(220, 90, 110, 125)
        stroke.strokeWidth = 2f
        c.drawLine(12f, base, w - 12f, base, stroke)
    }

    private fun drawTakeoff(c: Canvas, w: Float, h: Float) {
        // sky and runway strip
        paint.color = Color.argb(210, 190, 220, 245)
        c.drawRoundRect(5f, 4f, w - 5f, h - 4f, 8f, 8f, paint)
        paint.color = Color.rgb(115, 125, 130)
        c.drawRect(5f, h * 0.73f, w - 5f, h - 4f, paint)
        paint.color = Color.WHITE
        for (var x = 12f; x < w; x += 18f) c.drawRect(x, h * 0.84f, x + 9f, h * 0.88f, paint)
        val t = progress
        val x = 16f + (w - 34f) * t
        val lift = 2f + 32f * t * t
        val y = h * 0.68f - lift
        drawPlane(c, x, y, -0.18f * t, 1.0f, Color.WHITE)
        // tiny motion streaks
        stroke.color = Color.argb(130, 255, 255, 255)
        stroke.strokeWidth = 2f
        c.drawLine(x - 19f, y + 5f, x - 7f, y + 5f, stroke)
        c.drawLine(x - 15f, y + 10f, x - 5f, y + 10f, stroke)
    }

    private fun drawPlane(c: Canvas, cx: Float, cy: Float, angle: Float, scale: Float, color: Int) {
        c.save()
        c.translate(cx, cy)
        c.rotate(Math.toDegrees(angle.toDouble()).toFloat())
        c.scale(scale, scale)
        paint.color = color
        val body = Path().apply {
            moveTo(-22f, 0f); lineTo(-6f, -2f); lineTo(15f, -2f); lineTo(25f, 0f)
            lineTo(15f, 2f); lineTo(-6f, 2f); close()
        }
        c.drawPath(body, paint)
        val wing = Path().apply { moveTo(0f, -2f); lineTo(8f, -12f); lineTo(13f, -12f); lineTo(9f, -2f); close() }
        c.drawPath(wing, paint)
        val wing2 = Path().apply { moveTo(0f, 2f); lineTo(8f, 12f); lineTo(13f, 12f); lineTo(9f, 2f); close() }
        c.drawPath(wing2, paint)
        val tail = Path().apply { moveTo(-13f, -1f); lineTo(-18f, -8f); lineTo(-12f, -7f); lineTo(-7f, -1f); close() }
        c.drawPath(tail, paint)
        paint.color = Color.rgb(255, 170, 0)
        c.drawCircle(22f, 0f, 2.2f, paint)
        c.restore()
    }
}
