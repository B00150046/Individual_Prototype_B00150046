package com.example.recommenderbooks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recommenderbooks.network.BookMetadata

class BookAdapter(private val onBookClick: (String) -> Unit) : ListAdapter<BookMetadata, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.book_item, parent, false)
        return BookViewHolder(view, onBookClick)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = getItem(position)
        holder.bind(book)
    }

    class BookViewHolder(itemView: View, private val onBookClick: (String) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val bookCoverImageView: ImageView = itemView.findViewById(R.id.book_cover)
        private val bookTitleTextView: TextView = itemView.findViewById(R.id.book_title)

        fun bind(book: BookMetadata) {
            bookTitleTextView.text = book.name
            itemView.setOnClickListener { onBookClick(book.name) }
            
            Glide.with(itemView.context)
                .load(book.imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .into(bookCoverImageView)
        }
    }
}

class BookDiffCallback : DiffUtil.ItemCallback<BookMetadata>() {
    override fun areItemsTheSame(oldItem: BookMetadata, newItem: BookMetadata): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: BookMetadata, newItem: BookMetadata): Boolean {
        return oldItem == newItem
    }
}
