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
        val nameTextView: TextView = itemView.findViewById(R.id.library_name)
        val bookCountTextView: TextView = itemView.findViewById(R.id.book_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.library_item, parent, false)
        return LibraryViewHolder(view)
    }

    override fun onBindViewHolder(holder: LibraryViewHolder, position: Int) {
        val library = libraries[position]
        holder.nameTextView.text = library.name
        val bookCount = library.books.size
        holder.bookCountTextView.text = "$bookCount ${if (bookCount == 1) "Book" else "Books"}"
        holder.itemView.setOnClickListener { onItemClick(library) }
    }

    override fun getItemCount(): Int = libraries.size

    fun updateLibraries(newLibraries: List<Library>) {
        libraries = newLibraries
        notifyDataSetChanged()
    }
}
