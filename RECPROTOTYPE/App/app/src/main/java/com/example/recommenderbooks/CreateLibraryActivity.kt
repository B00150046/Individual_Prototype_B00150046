package com.example.recommenderbooks

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.recommenderbooks.model.Library
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference

class CreateLibraryActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_library)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val libraryNameEditText = findViewById<EditText>(R.id.edit_library_name)
        val createButton = findViewById<Button>(R.id.btn_create_library)

        createButton.setOnClickListener {
            val name = libraryNameEditText.text.toString().trim()
            val user = auth.currentUser

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a library name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (user == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create a new document in "Library" collection
            val librariesRef: DocumentReference = firestore.collection("Library").document()
            val library = Library(
                libraryId = librariesRef.id, // use the Firestore-generated ID
                userId = user.uid,
                name = name,
                books = emptyList() // initialize empty list for books
            )

            librariesRef.set(library)
                .addOnSuccessListener {
                    Toast.makeText(this, "Library created successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to create library: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}