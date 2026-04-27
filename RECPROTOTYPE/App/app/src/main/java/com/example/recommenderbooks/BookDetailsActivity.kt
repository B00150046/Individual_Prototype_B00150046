package com.example.recommenderbooks

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.recommenderbooks.model.Library
import com.example.recommenderbooks.network.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

class BookDetailsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var firestore: FirebaseFirestore
    private var currentBookId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_details)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val recommendButton = findViewById<Button>(R.id.recommend_button)
        val addToLibraryButton = findViewById<Button>(R.id.start_reading_button)
        addToLibraryButton.text = "Add to Library"
        
        val bookTitleTextView = findViewById<TextView>(R.id.book_title_details)
        val bookAuthorTextView = findViewById<TextView>(R.id.book_author_details)
        val bookDescriptionTextView = findViewById<TextView>(R.id.book_description)
        val bookCoverImageView = findViewById<ImageView>(R.id.book_cover_details)
        val userRatingBar = findViewById<RatingBar>(R.id.user_rating_bar)
        
        val bookTitle = intent.getStringExtra("BOOK_TITLE") ?: "Unknown Book"
        bookTitleTextView.text = bookTitle

        fetchBookMetadata(bookTitle, bookAuthorTextView, bookDescriptionTextView, bookCoverImageView, userRatingBar)

        userRatingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) {
                saveRating(bookTitle, rating)
                sendInteractionToModel(rating.toInt().toString() + "_star")
            }
        }

        recommendButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val response = RetrofitInstance.recommenderApi.getRecommendation(bookTitle, auth.currentUser?.uid ?: "guest_user")
                    Toast.makeText(this@BookDetailsActivity, "Found ${response.inner.recommendations.size} similar books", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("BookDetailsActivity", "Error fetching recommendations", e)
                }
            }
        }

        addToLibraryButton.setOnClickListener {
            showLibrarySelectionDialog(bookTitle)
        }
    }

    private fun sendInteractionToModel(action: String) {
        val userId = auth.currentUser?.uid ?: "guest_user"
        if (currentBookId == 0) return

        lifecycleScope.launch {
            try {
                RetrofitInstance.recommenderApi.interact(userId, currentBookId, action)
                Log.d("BookDetailsActivity", "Sent interaction to model: $action for book $currentBookId")
            } catch (e: Exception) {
                Log.e("BookDetailsActivity", "Failed to send interaction to model", e)
            }
        }
    }

    private fun fetchBookMetadata(
        title: String,
        authorTv: TextView,
        descTv: TextView,
        coverIv: ImageView,
        ratingBar: RatingBar
    ) {
        lifecycleScope.launch {
            try {
                val metadata = RetrofitInstance.recommenderApi.getBookDetails(title)
                currentBookId = metadata.id
                
                authorTv.text = "By ${metadata.author ?: "Unknown Author"}"
                
                val info = StringBuilder()
                metadata.publishDate?.let { info.append("Published: $it\n") }
                metadata.isbn?.let { info.append("ISBN: $it\n") }
                metadata.ratingCount?.let { info.append("Total Ratings: $it") }
                descTv.text = info.toString()
                
                metadata.avgRating?.let { ratingBar.rating = it.toFloat() }
                
                if (!metadata.imageUrl.isNullOrEmpty()) {
                    Glide.with(this@BookDetailsActivity)
                        .load(metadata.imageUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(coverIv)
                }
            } catch (e: Exception) {
                Log.e("BookDetailsActivity", "Failed to fetch metadata for $title", e)
                authorTv.text = "Details unavailable"
            }
        }
    }

    private fun showLibrarySelectionDialog(bookTitle: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("Library")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val libraries = snapshot.toObjects(Library::class.java)
                if (libraries.isEmpty()) {
                    Toast.makeText(this, "No libraries found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val libraryNames = libraries.map { it.name }.toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle("Select Library")
                    .setItems(libraryNames) { _, which ->
                        addBookToLibrary(libraries[which], bookTitle)
                        sendInteractionToModel("add_library")
                    }
                    .show()
            }
    }

    private fun addBookToLibrary(library: Library, bookTitle: String) {
        firestore.collection("Library").document(library.libraryId)
            .update("books", com.google.firebase.firestore.FieldValue.arrayUnion(mapOf("title" to bookTitle)))
            .addOnSuccessListener {
                Toast.makeText(this, "Added to ${library.name}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveRating(bookTitle: String, rating: Float) {
        val userId = auth.currentUser?.uid ?: return
        val ratingsRef = database.getReference("user_ratings").child(userId)
        val safeTitle = bookTitle.replace(".", ",").replace("#", "-").replace("$", "-").replace("[", "-").replace("]", "-")
        ratingsRef.child(safeTitle).setValue(rating)
    }
}
