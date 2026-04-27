package com.example.recommenderbooks

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recommenderbooks.network.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var recommendedBooksAdapter: RecommendedBookAdapter
    private lateinit var recommendedRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()

        val welcomeTextView = findViewById<TextView>(R.id.welcome_text)
        val profileButton = findViewById<ImageView>(R.id.profile_button)
        val recommenderButton = findViewById<Button>(R.id.recommender_button)
        recommendedRecyclerView = findViewById(R.id.recommended_recycler_view)

        setupRecyclerView()

        val user = auth.currentUser
        if (user != null) {
            val username = user.displayName
            welcomeTextView.text = if (!username.isNullOrEmpty()) "Good Morning, $username!" else "Good Morning!"
            fetchAndDisplayBooks()
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        profileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        recommenderButton.setOnClickListener {
            startActivity(Intent(this, RecommenderActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        recommendedBooksAdapter = RecommendedBookAdapter(emptyList()) { bookTitle ->
            val intent = Intent(this, BookDetailsActivity::class.java)
            intent.putExtra("BOOK_TITLE", bookTitle)
            startActivity(intent)
        }
        recommendedRecyclerView.adapter = recommendedBooksAdapter
        recommendedRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun fetchAndDisplayBooks() {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.recommenderApi.getAllBooks()
                val titlesToShow = response.books.take(30)
                recommendedBooksAdapter.updateBooks(titlesToShow)
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Failed to fetch books", e)
            }
        }
    }
}
