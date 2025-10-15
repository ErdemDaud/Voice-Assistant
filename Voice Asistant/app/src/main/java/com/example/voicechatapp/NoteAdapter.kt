package com.example.voicechatapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NoteAdapter(
    private val notes: List<String>,
    private val onDelete: (Int) -> Unit,
    private val onEdit: (Int) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val noteText: TextView = view.findViewById(R.id.textNote)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.noteText.text = notes[position]

        holder.btnDelete.setOnClickListener {
            onDelete(position)
        }

        holder.btnEdit.setOnClickListener {
            onEdit(position)
        }
    }

    override fun getItemCount(): Int = notes.size
}
