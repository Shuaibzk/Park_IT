package com.example.parkinglotbookingsystem

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val goToRegister = findViewById<TextView>(R.id.goToRegister)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            } else {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                            FirebaseFirestore.getInstance().collection("users").document(uid)
                                .get()
                                .addOnSuccessListener { document ->
                                    if (document.exists() && document.getBoolean("banned") == true) {
                                        auth.signOut()
                                        Toast.makeText(this, "You are banned from using this service.", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, HomeActivity::class.java))
                                        finish()
                                    }
                                }
                        } else {
                            Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }

            }
        }

        goToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}