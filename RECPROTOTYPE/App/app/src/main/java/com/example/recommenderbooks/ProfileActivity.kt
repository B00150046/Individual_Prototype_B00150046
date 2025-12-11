package com.example.recommenderbooks

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        val user = auth.currentUser
        val profileNameTextView = findViewById<TextView>(R.id.profile_name)
        val profileImageView = findViewById<ImageView>(R.id.profile_image)

        if (user != null) {
            val username = user.displayName
            val photoUrl = user.photoUrl

            if (!username.isNullOrEmpty()) {
                profileNameTextView.text = username
            } else {
                profileNameTextView.text = "User Name"
            }

            if (photoUrl != null) {
                Glide.with(this)
                    .load(photoUrl)
                    .into(profileImageView)
            } else {
                // Set a default image if the user has no profile picture
                profileImageView.setImageResource(R.drawable.ic_launcher_background)
            }
        } else {
            // If no user is logged in, redirect to the login screen
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
