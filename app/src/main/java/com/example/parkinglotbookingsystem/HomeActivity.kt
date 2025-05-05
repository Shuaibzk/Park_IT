package com.example.parkinglotbookingsystem

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            // Fetch role from Firestore
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val role = document.getString("role")

                        when (role) {
                            "client" -> {
                                startActivity(Intent(this, ClientHomeActivity::class.java))
                                finish()
                            }
                            "owner" -> {
                                startActivity(Intent(this, OwnerHomeActivity::class.java))
                                finish()
                            }
                            "admin" -> {
                                startActivity(Intent(this, AdminHomeActivity::class.java))
                                finish()
                            }
                            else -> {
                                Toast.makeText(this, "Unknown user role", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    } else {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error fetching user info", Toast.LENGTH_LONG).show()
                    finish()
                }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}