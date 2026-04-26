package com.example.recommenderbooks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recommenderbooks.model.Library

class LibraryAdapter(
    private var libraries: List<Library>,
    private val onItemClick: (Library) -> Unit
) : RecyclerView.Adapter<LibraryAdapter.LibraryViewHolder>() {

    class LibraryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return LibraryViewHolder(view)
    }

    override fun onBindViewHolder(holder: LibraryViewHolder, position: Int) {
        val library = libraries[position]
        holder.nameTextView.text = library.name
        holder.itemView.setOnClickListener { onItemClick(library) }
    }

    override fun getItemCount(): Int = libraries.size

    fun updateLibraries(newLibraries: List<Library>) {
        libraries = newLibraries
        notifyDataSetChanged()
    }
}
