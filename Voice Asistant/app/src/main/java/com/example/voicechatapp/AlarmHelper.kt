package com.example.voicechatapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object AlarmHelper {

    /**
     * Set alarm - handles format: DayName/YYYY-MM-DD/HH:MM
     * Example: "Monday/2025-01-15/15:30" or "Friday/2025-01-17/08:00"
     * LLM calculates the exact date/time with current time context, phone validates and sets it
     */
    fun setAlarm(context: Context, alarmData: String): String {
        try {
            val parts = alarmData.trim().split("/")

            Log.d("AlarmHelper", "Parsing alarm data: $alarmData")
            Log.d("AlarmHelper", "Parts count: ${parts.size}, Parts: ${parts.joinToString()}")

            // Expect exactly 3 parts: DayName/YYYY-MM-DD/HH:MM
            if (parts.size != 3) {
                Log.e("AlarmHelper", "Invalid format - expected 3 parts, got ${parts.size}")
                return "Invalid alarm format. Expected: DayName/YYYY-MM-DD/HH:MM"
            }

            val day = parts[0].trim()
            val dateStr = parts[1].trim()
            val timeStr = parts[2].trim()

            // Validate day name
            val validDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            if (!validDays.contains(day)) {
                Log.e("AlarmHelper", "Invalid day name: $day")
                return "Invalid day name: $day. Must be a full English day name (e.g., Monday)"
            }

            return setExactAlarm(context, day, dateStr, timeStr)

        } catch (e: Exception) {
            Log.e("AlarmHelper", "Error parsing alarm: ${e.message}", e)
            return "Error setting alarm: ${e.message}"
        }
    }

    /**
     * Set exact alarm using the calculated date/time from LLM
     * Format: DayName/YYYY-MM-DD/HH:MM
     * Example: Monday/2025-01-15/15:30
     *
     * The LLM now receives current time with every message, so it should
     * always calculate future times correctly. This function validates that.
     */
    private fun setExactAlarm(context: Context, day: String, dateStr: String, timeStr: String): String {
        try {
            // Parse the exact date and time provided by LLM
            val dateTimeStr = "$dateStr $timeStr"
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
            dateFormat.timeZone = TimeZone.getDefault() // Use phone's timezone

            val targetDate = dateFormat.parse(dateTimeStr)

            if (targetDate == null) {
                Log.e("AlarmHelper", "Failed to parse date: $dateTimeStr")
                return "Invalid date/time format. Expected YYYY-MM-DD HH:MM"
            }

            val calendar = Calendar.getInstance()
            calendar.time = targetDate

            val triggerTime = calendar.timeInMillis
            val currentTime = System.currentTimeMillis()

            val currentDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            Log.d("AlarmHelper", "Current time: ${currentDateFormat.format(Date(currentTime))}")
            Log.d("AlarmHelper", "Alarm time: ${currentDateFormat.format(Date(triggerTime))}")
            Log.d("AlarmHelper", "Difference (minutes): ${(triggerTime - currentTime) / 1000 / 60}")

            // Check if alarm is in the future
            if (triggerTime <= currentTime) {
                val diff = currentTime - triggerTime
                val diffMinutes = diff / 1000 / 60
                val diffHours = diff / 1000 / 60 / 60

                Log.e("AlarmHelper", "ERROR: Alarm time is in the past!")
                Log.e("AlarmHelper", "Time difference: $diffMinutes minutes ($diffHours hours)")

                // If it's very close (less than 1 minute), might be due to processing delay
                if (diff < 60 * 1000) {
                    Log.i("AlarmHelper", "Time difference is tiny (< 1 min), setting alarm anyway")
                } else {
                    // This should not happen anymore since LLM now knows current time
                    return "Cannot set alarm in the past! Alarm was for $diffMinutes minutes ago. " +
                           "Please try again with a future time."
                }
            }

            // Set the alarm
            setAlarmAtTime(context, triggerTime)

            // Format for display
            val displayFormat = SimpleDateFormat("EEEE, MMM dd 'at' hh:mm a", Locale.ENGLISH)
            val formattedTime = displayFormat.format(calendar.time)

            val minutesFromNow = (triggerTime - currentTime) / 1000 / 60
            val hoursFromNow = minutesFromNow / 60
            val remainingMinutes = minutesFromNow % 60

            val timeUntil = if (hoursFromNow > 0) {
                "$hoursFromNow hours and $remainingMinutes minutes"
            } else {
                "$minutesFromNow minutes"
            }

            return "Alarm set for $formattedTime in $timeUntil"

        } catch (e: Exception) {
            Log.e("AlarmHelper", "Error setting exact alarm: ${e.message}", e)
            return "Error setting alarm: ${e.message}"
        }
    }

    /**
     * Set alarm at a specific time (timestamp)
     * Uses phone's local timezone
     */
    private fun setAlarmAtTime(context: Context, triggerTime: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            triggerTime.toInt(), // Use timestamp as unique ID
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }
}
