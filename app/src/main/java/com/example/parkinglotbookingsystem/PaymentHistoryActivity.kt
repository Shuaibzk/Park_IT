package com.example.parkinglotbookingsystem

// client payment history

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PaymentHistoryActivity : AppCompatActivity() {

    private lateinit var listLayout: LinearLayout
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_history)

        listLayout = findViewById(R.id.paymentListLayout)

        loadPayments()
    }

    private fun loadPayments() {
        val clientId = auth.currentUser?.uid ?: return

        firestore.collection("pending_bookings")
            .whereEqualTo("clientId", clientId)

            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    val emptyText = TextView(this).apply {
                        text = "No payments found."
                        textSize = 16f
                        gravity = Gravity.CENTER
                        setPadding(0, 40, 0, 0)
                    }
                    listLayout.addView(emptyText)
                    return@addOnSuccessListener
                }

                for (doc in snapshot.documents) {
                    val lotNumber = doc.getString("lotNumber") ?: "Unknown"
                    val durationHours = doc.getLong("durationHours") ?: 0
                    val pricePerHour = doc.getDouble("pricePerHour") ?: 0.0
                    val totalPrice = pricePerHour * durationHours
                    val date = doc.getString("date") ?: "?"
                    val startTime = doc.getString("startTime") ?: "?"
                    val ownerId = doc.getString("ownerId") ?: ""

                    firestore.collection("users").document(ownerId).get()
                        .addOnSuccessListener { ownerDoc ->
                            val ownerName = ownerDoc.getString("name") ?: "Unknown Owner"
                            addPaymentCard(lotNumber, totalPrice, durationHours, date, startTime, ownerName)
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load payment history", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addPaymentCard(
        lotNumber: String,
        totalPrice: Double,
        durationHours: Long,
        date: String,
        startTime: String,
        ownerName: String
    ) {
        val context = this
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundResource(android.R.color.darker_gray)
            setBackgroundColor(0xFFE0E0E0.toInt())
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 24)
            layoutParams = params
        }

        fun infoRow(label: String, value: String): TextView {
            return TextView(context).apply {
                text = "$label $value"
                textSize = 16f
            }
        }

        card.addView(infoRow("Lot Number:", lotNumber))
        card.addView(infoRow("Amount Paid:", "%.2f BDT".format(totalPrice)))
        card.addView(infoRow("Duration:", "$durationHours hour(s)"))
        card.addView(infoRow("Date:", date))
        card.addView(infoRow("Start Time:", startTime))
        card.addView(infoRow("Owner:", ownerName))

        listLayout.addView(card)
    }
}
