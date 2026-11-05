package com.kodomo.app.kiosk

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import com.kodomo.app.core.util.PersianDigits
import kotlin.math.abs

/**
 * A floating «🏠 کدومو» button drawn over every parent-approved external app (needs «display over other apps»).
 * It shows the remaining screen time, can be dragged out of the way, and one tap returns the child to Kodomo.
 */
class ReturnBubble(private val context: Context, private val onReturn: () -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: TextView? = null
    private var params: WindowManager.LayoutParams? = null

    private fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), context.resources.displayMetrics).toInt()

    fun show(remainingSeconds: Long) = main.post {
        if (!Settings.canDrawOverlays(context)) return@post
        val text = "🏠 کدومو  ${PersianDigits.toPersian((remainingSeconds / 60).toInt())}′"
        view?.let { it.text = text; return@post }
        create(text)
    }

    fun hide() = main.post {
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun create(text: String) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(120)
        }
        val tv = TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dp(28).toFloat()
                colors = intArrayOf(Color.parseColor("#5FB98A"), Color.parseColor("#2E7D5B"))
                orientation = GradientDrawable.Orientation.TOP_BOTTOM
                setStroke(dp(2), Color.WHITE)
            }
            elevation = dp(8).toFloat()
        }
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        tv.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; startX = lp.x; startY = lp.y; true }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = startX + (e.rawX - downX).toInt()
                    lp.y = startY + (e.rawY - downY).toInt()
                    runCatching { windowManager.updateViewLayout(v, lp) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (abs(e.rawX - downX) < dp(8) && abs(e.rawY - downY) < dp(8)) onReturn()
                    true
                }
                else -> false
            }
        }
        if (runCatching { windowManager.addView(tv, lp) }.isSuccess) {
            view = tv
            params = lp
        }
    }
}
