package com.example.voicechatapp

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("NotesPrefs", Context.MODE_PRIVATE)

    private val gson = Gson()
    private val notesKey = "notes"

    fun saveNotes(notes: List<String>) {
        val json = gson.toJson(notes)
        sharedPreferences.edit().putString(notesKey, json).apply()
    }

    fun getNotesList(): List<String> {
        val json = sharedPreferences.getString(notesKey, null)
        return if (json != null) {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }
    fun addConversationNote(userMessage: String, response: String) {
        val timestamp = getCurrentTimestamp()
        val note = "[$timestamp] You: $userMessage\n[$timestamp] Bot: $response"
        val notes = getNotesList().toMutableList()
        notes.add(note)
        saveNotes(notes)
    }
    private fun getCurrentTimestamp(): String {
        val formatter = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date())
    }



}
