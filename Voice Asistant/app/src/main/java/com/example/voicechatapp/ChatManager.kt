package com.example.voicechatapp

import android.speech.tts.TextToSpeech

class ChatManager(
    private val chatHistory: MutableList<String>,
    private val chatAdapter: ChatAdapter,
    private val tts: TextToSpeech
) {

    fun addUserMessage(message: String) {
        chatHistory.add("🧑 You: $message")
        chatAdapter.notifyItemInserted(chatHistory.size - 1)
    }

    fun addAssistantMessage(message: String) {
        chatHistory.add("🤖 Assistant: $message")
        chatAdapter.notifyItemInserted(chatHistory.size - 1)
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun addSystemMessage(message: String) {
        chatHistory.add("📢 System: $message")
        chatAdapter.notifyItemInserted(chatHistory.size - 1)
    }

    fun addErrorMessage(error: String) {
        chatHistory.add("❗ Error: $error")
        chatAdapter.notifyItemInserted(chatHistory.size - 1)
    }
}
