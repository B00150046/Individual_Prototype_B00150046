package com.example.recommenderbooks

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.recommenderbooks.network.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class BookDetailsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_details)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        val recommendButton = findViewById<Button>(R.id.recommend_button)
        val bookTitleTextView = findViewById<TextView>(R.id.book_title_details)
        val userGreetingTextView = findViewById<TextView>(R.id.user_greeting_textview)

        // Set user greeting
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userName = currentUser.displayName
            if (!userName.isNullOrEmpty()) {
                userGreetingTextView.text = "Hello, $userName"
            } else {
                userGreetingTextView.text = "Hello, User"
            }
        } else {
            userGreetingTextView.text = "Hello, Guest"
        }

        recommendButton.setOnClickListener {
            val bookTitle = bookTitleTextView.text.toString()
            lifecycleScope.launch {
                try {
                    val recommendations = RetrofitInstance.api.getRecommendations(bookTitle)
                    // You can now display these recommendations in a new activity or a dialog
                    Log.d("BookDetailsActivity", "Recommendations: $recommendations")
                } catch (e: Exception) {
                    Log.e("BookDetailsActivity", "Error fetching recommendations", e)
                }
            }
        }
    }
}
