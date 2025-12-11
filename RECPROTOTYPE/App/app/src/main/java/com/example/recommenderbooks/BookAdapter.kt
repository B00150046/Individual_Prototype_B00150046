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
import com.example.recommenderbooks.network.Book

class BookAdapter : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        // Assumes you have a layout file named 'book_item.xml'
        val view = LayoutInflater.from(parent.context).inflate(R.layout.book_item, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = getItem(position)
        holder.bind(book)
    }

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Assumes your 'book_item.xml' has an ImageView with id 'book_cover' and a TextView with id 'book_title'
        private val bookCoverImageView: ImageView = itemView.findViewById(R.id.book_cover)
        private val bookTitleTextView: TextView = itemView.findViewById(R.id.book_title)

        fun bind(book: Book) {
            bookTitleTextView.text = book.name
            Glide.with(itemView.context)
                .load(book.imageUrl)
                .placeholder(R.drawable.ic_launcher_background) // Default image
                .into(bookCoverImageView)
        }
    }
}

class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
    override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
        return oldItem == newItem
    }
}
