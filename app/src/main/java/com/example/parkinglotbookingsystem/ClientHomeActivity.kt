package com.example.parkinglotbookingsystem

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClientHomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var bookParkingLotBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_home)

        val welcomeTextView = findViewById<TextView>(R.id.welcomeClient)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        bookParkingLotBtn = findViewById(R.id.bookParkingLotBtn)

        val uid = auth.currentUser?.uid

        if (uid != null) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name")
                        welcomeTextView.text = "Welcome, ${name ?: "Client"}!"
                    } else {
                        welcomeTextView.text = "Welcome, Client!"
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to fetch user info", Toast.LENGTH_SHORT).show()
                    welcomeTextView.text = "Welcome, Client!"
                }
        }

        bookParkingLotBtn.setOnClickListener {
            val intent = Intent(this, ClientSearchResultActivity::class.java)
            startActivity(intent)
        }

        val myBookingsBtn = findViewById<Button>(R.id.myBookingsBtn)
        myBookingsBtn.setOnClickListener {
            startActivity(Intent(this, MyBookingsActivity::class.java))
        }

        val generalReportBtn = findViewById<Button>(R.id.generalReportBtn)
        val reportOwnerBtn = findViewById<Button>(R.id.reportOwnerBtn)

        generalReportBtn.setOnClickListener {
            startActivity(Intent(this, ClientReportActivity::class.java).putExtra("reportType", "general"))
        }

        reportOwnerBtn.setOnClickListener {
            startActivity(Intent(this, ClientReportActivity::class.java).putExtra("reportType", "owner"))
        }

        val profileBtn = findViewById<Button>(R.id.profileBtn)
        profileBtn.setOnClickListener {
            startActivity(Intent(this, ClientProfileActivity::class.java))
        }

        val paymentHistoryBtn = findViewById<Button>(R.id.paymentHistoryBtn)
        paymentHistoryBtn.setOnClickListener {
            startActivity(Intent(this, PaymentHistoryActivity::class.java))
        }


    }
}