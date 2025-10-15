package com.example.voicechatapp

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.RelativeLayout
import android.widget.TextView

class VoiceRecordingDialog(context: Context) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    private lateinit var pulseCircle: View
    private lateinit var tvSpeechText: TextView
    private lateinit var rootLayout: RelativeLayout
    private var pulseAnimator: ObjectAnimator? = null
    private var onCancelListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_voice_recording)

        // Make fullscreen and transparent
        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        // Initialize views
        pulseCircle = findViewById(R.id.pulseCircle)
        tvSpeechText = findViewById(R.id.tvSpeechText)
        rootLayout = findViewById(R.id.rootLayout)

        // Tap anywhere to cancel
        rootLayout.setOnClickListener {
            onCancelListener?.invoke()
            dismiss()
        }

        // Start pulsating animation
        startPulseAnimation()
    }

    private fun startPulseAnimation() {
        // Create scale animation for the circle
        val scaleUpAnimator = ObjectAnimator.ofFloat(pulseCircle, "scaleX", 1.0f, 1.3f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        val scaleUpAnimatorY = ObjectAnimator.ofFloat(pulseCircle, "scaleY", 1.0f, 1.3f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Also animate alpha for breathing effect
        val alphaAnimator = ObjectAnimator.ofFloat(pulseCircle, "alpha", 1.0f, 0.6f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        scaleUpAnimator.start()
        scaleUpAnimatorY.start()
        alphaAnimator.start()

        pulseAnimator = scaleUpAnimator
    }

    fun updateSpeechText(text: String) {
        tvSpeechText.text = text
    }

    fun setOnCancelListener(listener: () -> Unit) {
        onCancelListener = listener
    }

    override fun dismiss() {
        pulseAnimator?.cancel()
        super.dismiss()
    }
}
