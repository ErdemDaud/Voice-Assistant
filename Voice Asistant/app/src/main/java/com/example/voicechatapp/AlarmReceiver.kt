package com.example.voicechatapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private var mediaPlayer: MediaPlayer? = null
        private var vibrator: Vibrator? = null

        fun stopMediaAndVibration() {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            vibrator?.cancel()
            vibrator = null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "⏰ Alarm Triggered!", Toast.LENGTH_SHORT).show()

        try {
            val activityIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(activityIntent)
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to launch AlarmRingingActivity", e)
        }
    }

}
