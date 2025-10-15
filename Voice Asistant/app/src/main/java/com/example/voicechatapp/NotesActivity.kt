package com.example.voicechatapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream

class NotesActivity : AppCompatActivity() {

    private lateinit var noteManager: NoteManager
    private lateinit var adapter: NoteAdapter
    private lateinit var notes: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        // ✅ Context doğru veriliyor
        noteManager = NoteManager(this)

        // ✅ Notları yükle
        notes = noteManager.getNotesList().toMutableList()

        // ✅ RecyclerView kur
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerNotes)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = NoteAdapter(
            notes,
            onDelete = { position -> confirmDelete(position) },
            onEdit = { position -> showEditDialog(position) }
        )
        recyclerView.adapter = adapter

        // ✅ Export butonu
        findViewById<Button>(R.id.btnExport).setOnClickListener {
            exportNotesToFile()
        }
    }

    private fun confirmDelete(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ ->
                notes.removeAt(position)
                noteManager.saveNotes(notes)
                adapter.notifyItemRemoved(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(position: Int) {
        val editText = EditText(this).apply {
            setText(notes[position])
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Note")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                notes[position] = editText.text.toString()
                noteManager.saveNotes(notes)
                adapter.notifyItemChanged(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportNotesToFile() {
        val file = File(getExternalFilesDir(null), "ExportedNotes.txt")
        val text = notes.joinToString(separator = "\n") { "- $it" }

        try {
            FileOutputStream(file).use {
                it.write(text.toByteArray())
            }
            AlertDialog.Builder(this)
                .setTitle("Export Successful")
                .setMessage("Notes exported to:\n${file.absolutePath}")
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            AlertDialog.Builder(this)
                .setTitle("Export Failed")
                .setMessage("Error: ${e.message}")
                .setPositiveButton("OK", null)
                .show()
        }
    }
}
