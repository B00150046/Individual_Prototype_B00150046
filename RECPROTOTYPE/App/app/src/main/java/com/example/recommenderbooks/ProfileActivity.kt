package com.example.recommenderbooks

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recommenderbooks.model.Library
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var libraryAdapter: LibraryAdapter
    private val librariesList = mutableListOf<Library>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val user = auth.currentUser
        val profileNameTextView = findViewById<TextView>(R.id.profile_name)
        val profileImageView = findViewById<ImageView>(R.id.profile_image)
        val addLibraryButton = findViewById<Button>(R.id.btn_add_library)
        val librariesRecyclerView = findViewById<RecyclerView>(R.id.libraries_recycler_view)

        if (user != null) {
            profileNameTextView.text = user.displayName ?: "User Name"

            if (user.photoUrl != null) {
                Glide.with(this).load(user.photoUrl).into(profileImageView)
            } else {
                profileImageView.setImageResource(R.drawable.ic_launcher_background)
            }

            // Setup RecyclerView
            libraryAdapter = LibraryAdapter(librariesList) { library ->
                val intent = Intent(this, LibraryDetailActivity::class.java)
                intent.putExtra("library_id", library.libraryId)
                intent.putExtra("library_name", library.name)
                startActivity(intent)
            }
            librariesRecyclerView.layoutManager = LinearLayoutManager(this)
            librariesRecyclerView.adapter = libraryAdapter

            fetchUserLibraries(user.uid)

        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        addLibraryButton.setOnClickListener {
            startActivity(Intent(this, CreateLibraryActivity::class.java))
        }
    }

    private fun fetchUserLibraries(userId: String) {
        // Removed .orderBy() to avoid needing a composite index in Firestore
        firestore.collection("Library")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ProfileActivity", "Error fetching libraries", error)
                    Toast.makeText(this, "Failed to load libraries: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    librariesList.clear()
                    for (doc in snapshot.documents) {
                        try {
                            val library = doc.toObject(Library::class.java)
                            if (library != null) {
                                librariesList.add(library)
                            }
                        } catch (e: Exception) {
                            Log.e("ProfileActivity", "Error parsing library", e)
                        }
                    }
                    // Sort the list locally by name
                    librariesList.sortBy { it.name.lowercase() }
                    libraryAdapter.updateLibraries(librariesList)
                }
            }
    }
}
