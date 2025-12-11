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
    // Switch to the simpler adapter that handles strings
    private lateinit var recommendedBooksAdapter: RecommendedBookAdapter
    private lateinit var recommendedRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()

        val user = auth.currentUser
        val welcomeTextView = findViewById<TextView>(R.id.welcome_text)
        val profileButton = findViewById<ImageView>(R.id.profile_button)
        val recommenderButton = findViewById<Button>(R.id.recommender_button)
        recommendedRecyclerView = findViewById(R.id.recommended_recycler_view)

        setupRecyclerView()

        if (user != null) {
            val username = user.displayName
            if (!username.isNullOrEmpty()) {
                welcomeTextView.text = "Good Morning, $username!"
            } else {
                welcomeTextView.text = "Good Morning!"
            }
            // Fetch books for the recommended section
            fetchAndDisplayBooks()
        } else {
            // If no user is logged in, redirect to the login screen
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        recommenderButton.setOnClickListener {
            val intent = Intent(this, RecommenderActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        // Use the adapter designed for strings
        recommendedBooksAdapter = RecommendedBookAdapter(emptyList())
        recommendedRecyclerView.adapter = recommendedBooksAdapter
        recommendedRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun fetchAndDisplayBooks() {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.recommenderApi.getAllBooks()
                // The response now contains a list of strings
                val allBookTitles = response.books
                val titlesToShow = allBookTitles.take(30)
                // Use the update method for this adapter
                recommendedBooksAdapter.updateBooks(titlesToShow)
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Failed to fetch books (Ask Gemini)", e)
            }
        }
    }
}
