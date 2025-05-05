package com.example.parkinglotbookingsystem
// client review page logic

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ReviewActivity : AppCompatActivity() {

    private lateinit var ratingBar: RatingBar
    private lateinit var reviewEditText: EditText
    private lateinit var submitButton: Button

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        ratingBar = findViewById(R.id.ratingBar)
        reviewEditText = findViewById(R.id.reviewEditText)
        submitButton = findViewById(R.id.submitReviewBtn)

        val lotNumber = intent.getStringExtra("lotNumber") ?: return
        val ownerId = intent.getStringExtra("ownerId") ?: return
        val clientId = auth.currentUser?.uid ?: return

        submitButton.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            val reviewText = reviewEditText.text.toString().trim()

            if (rating == 0) {
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val reviewData = hashMapOf(
                "clientId" to clientId,
                "ownerId" to ownerId,
                "lotNumber" to lotNumber,
                "rating" to rating,
                "review" to reviewText,
                "timestamp" to FieldValue.serverTimestamp()
            )

            firestore.collection("reviews")
                .add(reviewData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Thanks for your feedback!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, ClientHomeActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to submit review", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
