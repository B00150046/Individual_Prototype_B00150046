package com.example.recommenderbooks

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val nameEditText = findViewById<EditText>(R.id.register_name)
        val passwordEditText = findViewById<EditText>(R.id.register_password)
        val emailEditText = findViewById<EditText>(R.id.register_email)
        val registerButton = findViewById<Button>(R.id.button_register)
        val loginButton = findViewById<TextView>(R.id.button_login)

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val userId = user?.uid
                        
                        // Set the display name immediately
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()

                        user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                val userProfile = hashMapOf(
                                    "name" to name,
                                    "email" to email,
                                    "userId" to userId,
                                    "createdAt" to com.google.firebase.Timestamp.now()
                                )

                                if (userId != null) {
                                    firestore.collection("users").document(userId)
                                        .set(userProfile)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                                            val intent = Intent(this, DashboardActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("RegisterActivity", "Firestore error: ${e.message}", e)
                                            // Proceed to dashboard even if firestore fails, as user is authenticated
                                            val intent = Intent(this, DashboardActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                }
                            }
                        }
                    } else {
                        Log.e("RegisterActivity", "Auth error: ${task.exception?.message}", task.exception)
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}
