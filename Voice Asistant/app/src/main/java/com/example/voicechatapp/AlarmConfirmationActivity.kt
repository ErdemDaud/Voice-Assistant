package com.example.voicechatapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Vibrator
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AlarmConfirmationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_confirmation)

        val time = intent.getStringExtra("alarm_time") ?: "Unknown"
        val day = intent.getStringExtra("alarm_day") ?: "Unknown"

        findViewById<TextView>(R.id.txtTime).text = time
        findViewById<TextView>(R.id.txtDay).text = day

        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            alarmManager.cancel(pendingIntent)

            // Stop any media or vibration
            AlarmReceiver.stopMediaAndVibration()

            finish()
        }
    }
}
