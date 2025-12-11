package com.example.recommenderbooks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecommendedBookAdapter(private var books: List<String>) : RecyclerView.Adapter<RecommendedBookAdapter.BookViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.book_item, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val bookTitle = books[position]
        holder.bookTitle.text = bookTitle
        // We don't have image URLs, so we'll just use a placeholder
        Glide.with(holder.itemView.context)
            .load(R.drawable.ic_launcher_background) // Or any other placeholder
            .into(holder.bookImage)
    }

    override fun getItemCount() = books.size

    fun updateBooks(newBooks: List<String>) {
        this.books = newBooks
        notifyDataSetChanged()
    }

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookImage: ImageView = itemView.findViewById(R.id.book_cover)
        val bookTitle: TextView = itemView.findViewById(R.id.book_title)
    }
}