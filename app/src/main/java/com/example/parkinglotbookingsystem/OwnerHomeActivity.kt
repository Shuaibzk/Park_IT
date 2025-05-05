package com.example.parkinglotbookingsystem

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OwnerHomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owner_home)

        val welcomeTextView = findViewById<TextView>(R.id.welcomeOwner)
        val registerLotBtn = findViewById<Button>(R.id.registerLotBtn)
        val viewMyLotsBtn = findViewById<Button>(R.id.viewMyLotsBtn) // ✅ new button

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val uid = auth.currentUser?.uid

        if (uid != null) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name")
                        welcomeTextView.text = "Welcome, ${name ?: "Owner"}!"
                    } else {
                        welcomeTextView.text = "Welcome, Owner!"
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to fetch user info", Toast.LENGTH_SHORT).show()
                    welcomeTextView.text = "Welcome, Owner!"
                }
        }

        // When "Register New Parking Lot" button is clicked
        registerLotBtn.setOnClickListener {
            val intent = Intent(this, RegisterParkingLotActivity::class.java)
            startActivity(intent)
        }

        // When "View My Parking Lots" button is clicked
        viewMyLotsBtn.setOnClickListener {
            val intent = Intent(this, ViewMyParkingLotsActivity::class.java)
            startActivity(intent)
        }

        // when pressing profile button
        val profileBtn = findViewById<Button>(R.id.ownerProfileBtn)
        profileBtn.setOnClickListener {
            val intent = Intent(this, OwnerProfileActivity::class.java)
            startActivity(intent)
        }

        // when pressing booking history button
        val bookingHistoryBtn = findViewById<Button>(R.id.bookingHistoryBtn)
        bookingHistoryBtn.setOnClickListener {
            val intent = Intent(this, OwnerBookingHistoryActivity::class.java)
            startActivity(intent)
        }

        // report
        val generalOwnerReportBtn = findViewById<Button>(R.id.generalOwnerReportBtn)
        val clientReportBtn = findViewById<Button>(R.id.clientReportBtn)

        generalOwnerReportBtn.setOnClickListener {
            startActivity(Intent(this, OwnerReportActivity::class.java).putExtra("reportType", "general"))
        }

        clientReportBtn.setOnClickListener {
            startActivity(Intent(this, OwnerReportActivity::class.java).putExtra("reportType", "client"))
        }

    }
}