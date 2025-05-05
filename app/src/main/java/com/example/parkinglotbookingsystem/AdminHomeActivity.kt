package com.example.parkinglotbookingsystem

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class AdminHomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_home)

        val welcomeTextView = findViewById<TextView>(R.id.welcomeAdmin)

        val manageUsersBtn = findViewById<Button>(R.id.manageUsersBtn)
        val approveParkingLotsBtn = findViewById<Button>(R.id.approveParkingLotsBtn)
        val verifyComplaintsBtn = findViewById<Button>(R.id.verifyComplaintsBtn)
        val banUserBtn = findViewById<Button>(R.id.banUserBtn)
        val manageLotsBtn = findViewById<Button>(R.id.manageLotsBtn)
        val logoutBtn = findViewById<Button>(R.id.logoutBtn)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val uid = auth.currentUser?.uid

        if (uid != null) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name")
                        welcomeTextView.text = "Welcome, ${name ?: "Admin"}!"
                    } else {
                        welcomeTextView.text = "Welcome, Admin!"
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to fetch user info", Toast.LENGTH_SHORT).show()
                    welcomeTextView.text = "Welcome, Admin!"
                }
        }

        //  Now connect the Approve Parking Lots button
        approveParkingLotsBtn.setOnClickListener {
            val intent = Intent(this, ApproveParkingLotsActivity::class.java)
            startActivity(intent)
        }


        verifyComplaintsBtn.setOnClickListener {
            startActivity(Intent(this, AdminVerifyComplaintsActivity::class.java))
        }

        logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        banUserBtn.setOnClickListener {
            startActivity(Intent(this, AdminBanUsersActivity::class.java))
        }

        manageUsersBtn.setOnClickListener {
            startActivity(Intent(this, ViewUsersActivity::class.java))
        }
        manageLotsBtn.setOnClickListener {
            startActivity(Intent(this, ManageParkingLotsActivity::class.java))
        }


    }
}