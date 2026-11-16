package com.yuri.alarme1236

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView

class RingActivity : Activity() {

    private var pulse: ObjectAnimator? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val label = intent.getStringExtra("label") ?: "Alarme"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#1B1F4B"), Color.parseColor("#0A0B1E"))
            )
            setPadding(dp(32), dp(32), dp(32), dp(32))
        }

        root.addView(TextView(this).apply {
            text = "ALARME"
            textSize = 14f
            letterSpacing = 0.2f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#9CA2C9"))
            gravity = Gravity.CENTER
        })

        root.addView(TextClock(this).apply {
            format24Hour = "HH:mm"
            format12Hour = "HH:mm"
            textSize = 84f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(4))
        })

        root.addView(TextView(this).apply {
            text = label
            textSize = 26f
            setTextColor(Color.parseColor("#C9CDFF"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(72))
        })

        val stop = TextView(this).apply {
            text = "PARAR"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0D0F1F"))
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#8E9BFF"))
            }
            setOnClickListener {
                startService(
                    Intent(this@RingActivity, RingService::class.java)
                        .setAction(RingService.ACTION_STOP)
                )
                finish()
            }
        }
        root.addView(stop, LinearLayout.LayoutParams(dp(180), dp(180)))

        pulse = ObjectAnimator.ofPropertyValuesHolder(
            stop,
            PropertyValuesHolder.ofFloat("scaleX", 1f, 1.08f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 1.08f)
        ).apply {
            duration = 700
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        setContentView(root)
    }

    override fun onDestroy() {
        pulse?.cancel()
        super.onDestroy()
    }
}
