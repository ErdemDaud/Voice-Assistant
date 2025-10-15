package com.example.voicechatapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chatManager: ChatManager
    private lateinit var editTextMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnSpeak: Button
    private lateinit var btnShowNotes: Button
    private lateinit var btnSettings: ImageButton
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var welcomeText: TextView
    private var voiceDialog: VoiceRecordingDialog? = null

    private val chatHistory = mutableListOf<String>()

    // OkHttpClient with longer timeout for LLM responses
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)  // Connection timeout
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)    // Write timeout
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)    // Read timeout - 2 minutes for LLM
        .build()

    private val RECORD_AUDIO_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        editTextMessage = findViewById(R.id.editTextMessage)
        btnSend = findViewById(R.id.btnSend)
        btnSpeak = findViewById(R.id.btnSpeak)
        btnShowNotes = findViewById(R.id.btnShowNotes)
        btnSettings = findViewById(R.id.btnSettings)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        welcomeText = findViewById(R.id.welcomeText)

        // Setup RecyclerView
        chatAdapter = ChatAdapter(chatHistory)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter

        // Initialize TTS
        tts = TextToSpeech(this, this)

        // Initialize Speech Recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        // Initialize ChatManager
        chatManager = ChatManager(chatHistory, chatAdapter, tts)

        // Check permissions
        checkAudioPermission()

        // Setup button listeners
        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        // Send button
        btnSend.setOnClickListener {
            val message = editTextMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                editTextMessage.text.clear()
            } else {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            }
        }

        // Speak button
        btnSpeak.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognition()
            } else {
                checkAudioPermission()
            }
        }

        // Show Notes button
        btnShowNotes.setOnClickListener {
            val intent = Intent(this, NotesActivity::class.java)
            startActivity(intent)
        }

        // Settings button
        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied. Cannot use voice features.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startVoiceRecognition() {
        // Show the voice recording dialog
        voiceDialog = VoiceRecordingDialog(this)
        voiceDialog?.setOnCancelListener {
            speechRecognizer.stopListening()
        }
        voiceDialog?.show()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // Enable partial results for real-time display
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                // Dialog is already showing
            }

            override fun onBeginningOfSpeech() {
                // User started speaking
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                // User stopped speaking
            }

            override fun onError(error: Int) {
                voiceDialog?.dismiss()
                Toast.makeText(this@MainActivity, "Error recognizing speech", Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    voiceDialog?.dismiss()
                    // Send message directly without showing in input box
                    sendMessage(recognizedText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Update dialog with partial results (real-time word display)
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    voiceDialog?.updateSpeechText(matches[0])
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    private fun sendMessage(message: String) {
        // Add user message to chat
        chatManager.addUserMessage(message)

        // Hide welcome text after first message
        welcomeText.visibility = TextView.GONE

        // Scroll to bottom
        chatRecyclerView.scrollToPosition(chatHistory.size - 1)

        // Send to server
        sendMessageToServer(message)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    private fun sendMessageToServer(message: String) {
        // Get server settings from SharedPreferences
        val prefs = getSharedPreferences("ServerSettings", MODE_PRIVATE)
        val serverIp = prefs.getString("server_ip", "10.64.195.116") ?: "10.64.195.116"
        val serverPort = prefs.getInt("server_port", 5000)
        val serverUrl = "http://$serverIp:$serverPort/receive"

        // Append current date and time to the message for LLM context
        val currentDateTime = getCurrentDateTime()
        val messageWithTime = "$message [Current time: $currentDateTime]"

        // Send as form data to match server expectations
        val formBody = FormBody.Builder()
            .add("message", messageWithTime)
            .build()

        val request = Request.Builder()
            .url(serverUrl)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    chatManager.addErrorMessage("Server connection failed: ${e.message}")
                    chatRecyclerView.scrollToPosition(chatHistory.size - 1)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                try {
                    val jsonResponse = JSONObject(responseBody)
                    val responseText = jsonResponse.getString("response").trim()
                    runOnUiThread {
                        handleLlamaResponse(responseText)
                        chatRecyclerView.scrollToPosition(chatHistory.size - 1)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        chatManager.addErrorMessage("Failed to parse server response: ${e.message}")
                        chatRecyclerView.scrollToPosition(chatHistory.size - 1)
                    }
                }
            }
        })
    }

    private fun handleLlamaResponse(response: String) {
        when {
            response.startsWith("XX1") -> {
                // Handle alarm setting (XX1 [minutes]/[day]/[date] or XX1 [minutes])
                val alarmData = response.removePrefix("XX1").trim()
                if (alarmData.isNotEmpty()) {
                    val result = AlarmHelper.setAlarm(this, alarmData)
                    chatManager.addSystemMessage(result)
                    speakOut(result)
                } else {
                    chatManager.addErrorMessage("Failed to set alarm")
                    speakOut("Sorry, I couldn't set the alarm.")
                }
            }
            response.startsWith("XX2") -> {
                // Handle note taking (XX2 [note content])
                val noteContent = response.removePrefix("XX2").trim()
                if (noteContent.isNotEmpty()) {
                    val noteManager = NoteManager(this)
                    val notes = noteManager.getNotesList().toMutableList()
                    notes.add(noteContent)
                    noteManager.saveNotes(notes)
                    chatManager.addSystemMessage("Note saved: $noteContent")
                    speakOut("Note saved successfully.")
                } else {
                    chatManager.addErrorMessage("Cannot save empty note")
                    speakOut("Sorry, I couldn't save an empty note.")
                }
            }
            else -> {
                // Normal conversation response
                chatManager.addAssistantMessage(response)
            }
        }
    }

    private fun speakOut(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    /**
     * Get current date and time in a format that LLM can easily understand
     * Format: DD/MM/YYYY HH:MM (Day: DayName)
     * Example: 15/01/2025 14:30 (Day: Monday)
     */
    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm (EEEE)", Locale.ENGLISH)
        return dateFormat.format(Date())
    }

    override fun onDestroy() {
        tts.shutdown()
        speechRecognizer.destroy()
        super.onDestroy()
    }
}
