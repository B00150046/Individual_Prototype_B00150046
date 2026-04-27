package com.example.recommenderbooks

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recommenderbooks.network.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecommenderActivity : AppCompatActivity() {

    private lateinit var fastapiCommandInput: EditText
    private lateinit var getRecommendationButton: Button
    private lateinit var recommendationRecyclerView: RecyclerView
    private lateinit var recommendedBookAdapter: RecommendedBookAdapter
    private lateinit var auth: FirebaseAuth

    // Use the centralized service
    private val recommenderService = RetrofitInstance.recommenderApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recommender)

        auth = FirebaseAuth.getInstance()
        fastapiCommandInput = findViewById(R.id.fastapi_command_input)
        getRecommendationButton = findViewById(R.id.get_recommendation_button)
        recommendationRecyclerView = findViewById(R.id.recommendation_recyclerview)

        recommendedBookAdapter = RecommendedBookAdapter(emptyList()) { bookTitle ->
            val intent = Intent(this, BookDetailsActivity::class.java)
            intent.putExtra("BOOK_TITLE", bookTitle)
            startActivity(intent)
        }
        recommendationRecyclerView.adapter = recommendedBookAdapter
        // Change the layout manager to a 2-column vertical grid
        recommendationRecyclerView.layoutManager = GridLayoutManager(this, 2)

        getRecommendationButton.setOnClickListener {
            val command = fastapiCommandInput.text.toString().trim()
            val userId = auth.currentUser?.uid
            
            if (command.isNotEmpty()) {
                if (userId != null) {
                    getRecommendation(command, userId)
                } else {
                    Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getRecommendation(command: String, userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // The response is now correctly typed and passes both book name and user ID
                val response = recommenderService.getRecommendation(command, userId)
                // Access the list directly from the parsed object
                val bookTitles = response.inner.recommendations
                withContext(Dispatchers.Main) {
                    recommendedBookAdapter.updateBooks(bookTitles)
                }
            } catch (e: Exception) {
                Log.e("RecommenderActivity", "Error getting recommendation", e)
                withContext(Dispatchers.Main) {
                    val errorList = listOf("Error: ${e.message}")
                    recommendedBookAdapter.updateBooks(errorList)
                }
            }
        }
    }
}
