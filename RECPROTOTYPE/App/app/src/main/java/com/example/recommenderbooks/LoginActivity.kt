package com.example.recommenderbooks

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthSettings

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        
        // Try to disable reCAPTCHA for the emulator
        val settings: FirebaseAuthSettings = auth.firebaseAuthSettings
        settings.forceRecaptchaFlowForTesting(false)

        val emailEditText = findViewById<TextInputEditText>(R.id.login_email)
        val passwordEditText = findViewById<TextInputEditText>(R.id.login_password)
        val loginButton = findViewById<Button>(R.id.button_login)
        val registerButton = findViewById<TextView>(R.id.button_register)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter credentials", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // --- TESTING BYPASS ---
            if (password == "test") {
                Log.d("LoginActivity", "Using bypass for testing")
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
                return@setOnClickListener
            }
            // -----------------------

            loginButton.isEnabled = false
            loginButton.text = "Authenticating..."

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    loginButton.isEnabled = true
                    loginButton.text = "Login"
                    
                    if (task.isSuccessful) {
                        Log.d("LoginActivity", "Login success")
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    } else {
                        val error = task.exception?.message ?: "Check your internet connection"
                        Log.e("LoginActivity", "Login error: $error")
                        Toast.makeText(this, "Login Failed: $error", Toast.LENGTH_LONG).show()
                    }
                }
        }

        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
