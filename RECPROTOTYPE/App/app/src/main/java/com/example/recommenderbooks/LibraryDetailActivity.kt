package com.example.recommenderbooks

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recommenderbooks.model.Library
import com.google.firebase.firestore.FirebaseFirestore

class LibraryDetailActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var bookAdapter: RecommendedBookAdapter
    private val booksList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library_detail)

        firestore = FirebaseFirestore.getInstance()

        val libraryId = intent.getStringExtra("library_id") ?: ""
        val libraryName = intent.getStringExtra("library_name") ?: "Library"

        findViewById<TextView>(R.id.library_name_title).text = libraryName

        val recyclerView = findViewById<RecyclerView>(R.id.library_books_recycler_view)
        
        // Setup Adapter with click listener
        bookAdapter = RecommendedBookAdapter(booksList) { bookTitle ->
            val intent = Intent(this, BookDetailsActivity::class.java)
            intent.putExtra("BOOK_TITLE", bookTitle)
            startActivity(intent)
        }
        
        // Use GridLayoutManager with 2 columns to match the recommender style
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = bookAdapter

        if (libraryId.isNotEmpty()) {
            fetchLibraryBooks(libraryId)
        } else {
            Toast.makeText(this, "Error: Library not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun fetchLibraryBooks(libraryId: String) {
        firestore.collection("Library").document(libraryId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("LibraryDetailActivity", "Error fetching books", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val library = snapshot.toObject(Library::class.java)
                    if (library != null) {
                        booksList.clear()
                        // Extract book titles from the list of maps
                        for (bookMap in library.books) {
                            val title = bookMap["title"]
                            if (title != null) {
                                booksList.add(title)
                            }
                        }
                        bookAdapter.updateBooks(booksList)
                    }
                }
            }
    }
}
