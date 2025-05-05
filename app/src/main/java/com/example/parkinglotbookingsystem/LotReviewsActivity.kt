package com.example.parkinglotbookingsystem
// view my parking lot(owner) -> review button


import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class LotReviewsActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var reviewsContainer: LinearLayout
    private lateinit var noReviewsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lot_reviews)

        firestore = FirebaseFirestore.getInstance()
        reviewsContainer = findViewById(R.id.reviewsContainer)

        noReviewsText = TextView(this).apply {
            text = "No reviews available for this parking lot."
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.darker_gray))
            setPadding(16, 32, 16, 16)
        }

        val lotNumber = intent.getStringExtra("lotNumber")

        if (lotNumber == null) {
            Toast.makeText(this, "Missing lot number", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadReviewsForLot(lotNumber)
    }

    private fun loadReviewsForLot(lotNumber: String) {
        firestore.collection("reviews")
            .whereEqualTo("lotNumber", lotNumber)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    reviewsContainer.addView(noReviewsText)
                    return@addOnSuccessListener
                }

                for (doc in result.documents) {
                    val message = doc.getString("review") ?: continue
                    val rating = doc.getDouble("rating") ?: continue
                    val clientId = doc.getString("clientId") ?: continue
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()
                    val formattedTime = timestamp?.let {
                        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it)
                    } ?: "Unknown time"



                    // Fetch client name
                    firestore.collection("users").document(clientId).get()
                        .addOnSuccessListener { userDoc ->
                            val clientName = userDoc.getString("name") ?: "Anonymous"

                            val reviewText = TextView(this).apply {
                                text = "⭐ $rating by $clientName\n$message\n🕒 $formattedTime"
                                textSize = 16f
                                setPadding(12, 16, 12, 16)
                            }

                            reviewsContainer.addView(reviewText)
                        }
                        .addOnFailureListener {
                            // Show with default name if failed
                            val reviewText = TextView(this).apply {
                                text = "⭐ $rating by Unknown\n$message\n🕒 $formattedTime"
                                textSize = 16f
                                setPadding(12, 16, 12, 16)
                            }

                            reviewsContainer.addView(reviewText)
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load reviews", Toast.LENGTH_SHORT).show()
            }
    }
}
