package com.example.voicechatapp

import android.media.MediaPlayer
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AlarmRingingActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_ringing)

        // 🔊 Play music
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm)
        mediaPlayer?.apply {
            isLooping = true
            start()
        }

        // 📳 Vibrate
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 500, 500, 500)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // repeat

        // 🔴 Dismiss Button
        findViewById<Button>(R.id.btnStopAlarm).setOnClickListener {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            vibrator?.cancel()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        vibrator?.cancel()
    }
}


