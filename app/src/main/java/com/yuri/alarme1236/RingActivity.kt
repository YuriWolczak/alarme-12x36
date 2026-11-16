package com.yuri.alarme1236

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class RingActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val label = intent.getStringExtra("label") ?: "Alarme"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#111111"))
            setPadding(48, 48, 48, 48)
        }
        root.addView(TextView(this).apply {
            text = "⏰"
            textSize = 72f
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = label
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 64)
        })
        root.addView(Button(this).apply {
            text = "PARAR"
            textSize = 24f
            setOnClickListener {
                startService(
                    Intent(this@RingActivity, RingService::class.java)
                        .setAction(RingService.ACTION_STOP)
                )
                finish()
            }
        })
        setContentView(root)
    }
}
