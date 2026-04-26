package com.example.recommenderbooks

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LibraryDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library_detail)

        val libraryName = intent.getStringExtra("library_name") ?: "Library"
        val bookIds = intent.getIntegerArrayListExtra("book_ids") ?: arrayListOf<Int>()

        findViewById<TextView>(R.id.library_name_title).text = libraryName

        val recyclerView = findViewById<RecyclerView>(R.id.library_books_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // We can reuse the RecommendedBookAdapter since it takes a List<String>
        // But we have bookIds (Ints). For now, let's just display the IDs as strings
        // until we have a way to fetch book names by IDs.
        val adapter = RecommendedBookAdapter(bookIds.map { "Book ID: $it" })
        recyclerView.adapter = adapter
    }
}
