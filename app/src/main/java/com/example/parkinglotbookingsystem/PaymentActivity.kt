package com.example.parkinglotbookingsystem

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class PaymentActivity : AppCompatActivity() {

    private lateinit var timerText: TextView
    private lateinit var homeBtn: Button
    private lateinit var cancelBtn: Button

    private lateinit var firestore: FirebaseFirestore
    private var bookingDocId: String? = null
    private var lotNumber: String? = null
    private var selectedSpaces: Int = 1

    private var timer: CountDownTimer? = null
    private val countdownTime = 5 * 60 * 1000L // 5 minutes in ms

    private lateinit var confirmBookingBtn: Button
    private lateinit var totalPriceText: TextView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        timerText = findViewById(R.id.timerText)
        homeBtn = findViewById(R.id.homeBtn)
        cancelBtn = findViewById(R.id.cancelBtn)

        firestore = FirebaseFirestore.getInstance()
        confirmBookingBtn = findViewById(R.id.confirmBookingBtn)

        totalPriceText = findViewById(R.id.totalPriceText)



        // Get passed values
        bookingDocId = intent.getStringExtra("bookingDocId")
        lotNumber = intent.getStringExtra("lotNumber")
        selectedSpaces = intent.getIntExtra("spacesBooked", 1)

        if (bookingDocId == null || lotNumber == null) {
            Toast.makeText(this, "Missing booking data", Toast.LENGTH_SHORT).show()
            finish()
        }

//        firestore.collection("pending_bookings").document(bookingDocId!!)
//            .get()
//            .addOnSuccessListener { document ->
//                if (document.exists()) {
//                    val totalPrice = document.getDouble("totalPrice")
//                    if (totalPrice != null) {
//                        totalPriceText.text = "Total Price: ${"%.2f".format(totalPrice)} BDT"
//                    } else {
//                        totalPriceText.text = "Total Price: Not Available"
//                    }
//                } else {
//                    totalPriceText.text = "Booking not found"
//                }
//            }
//            .addOnFailureListener {
//                totalPriceText.text = "Failed to load price"
//            }

        firestore.collection("pending_bookings").document(bookingDocId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val totalPrice = document.getDouble("totalPrice")
                    totalPrice?.let {
                        totalPriceText.text = "Total Price: ${"%.2f".format(it)} BDT"
                    }

                    val createdAt = document.getLong("timestamp") ?: 0L
                    val elapsed = System.currentTimeMillis() - createdAt
                    val remaining = 5 * 60 * 1000L - elapsed

                    if (remaining > 0) {
                        startTimer(remaining)
                    } else {
                        showTimeoutDialog()
                        deleteBookingAndRestoreSpaces()
                    }
                }
            }



//        startTimer()

//        confirmBookingBtn.setOnClickListener {
//            if (bookingDocId == null) {
//                Toast.makeText(this, "No booking to confirm", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            val paymentDate = System.currentTimeMillis()
//            val formattedPaymentDate = android.text.format.DateFormat.format("dd/MM/yyyy hh:mm a", paymentDate).toString()
//
//            timer?.cancel()
//
//            firestore.collection("pending_bookings")
//                .document(bookingDocId!!)
//                .update(
//                    mapOf(
//                        "status" to "confirmed",
//                        "payment_date" to formattedPaymentDate
//                    )
//                )
//                .addOnSuccessListener {
//                    Toast.makeText(this, "Booking confirmed!", Toast.LENGTH_SHORT).show()
//                    goToHomePage()
//                }
//                .addOnFailureListener {
//                    Toast.makeText(this, "Failed to confirm booking", Toast.LENGTH_SHORT).show()
//                }
//
//        }

        confirmBookingBtn.setOnClickListener {
            if (bookingDocId == null) {
                Toast.makeText(this, "No booking to confirm", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val paymentDate = System.currentTimeMillis()
            val formattedPaymentDate = android.text.format.DateFormat.format("dd/MM/yyyy hh:mm a", paymentDate).toString()

            timer?.cancel()

            firestore.collection("pending_bookings").document(bookingDocId!!)
                .get()
                .addOnSuccessListener { document ->
                    val ownerId = document.getString("ownerId") ?: ""

                    firestore.collection("pending_bookings").document(bookingDocId!!)
                        .update(
                            mapOf(
                                "status" to "confirmed",
                                "payment_date" to formattedPaymentDate
                            )
                        )
                        .addOnSuccessListener {
                            Toast.makeText(this, "Booking confirmed!", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this, ReviewActivity::class.java).apply {
                                putExtra("lotNumber", lotNumber)
                                putExtra("ownerId", ownerId)
                            }
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to confirm booking", Toast.LENGTH_SHORT).show()
                        }
                }
        }



        homeBtn.setOnClickListener {
            goToHomePage()
        }

        cancelBtn.setOnClickListener {
            cancelBooking()
        }
    }

//    private fun startTimer() {
//        timer = object : CountDownTimer(countdownTime, 1000) {
//            override fun onTick(millisUntilFinished: Long) {
//                val minutes = (millisUntilFinished / 1000) / 60
//                val seconds = (millisUntilFinished / 1000) % 60
//                timerText.text = String.format("Time left: %02d:%02d", minutes, seconds)
//            }
//
//            override fun onFinish() {
//                showTimeoutDialog()
//                deleteBookingAndRestoreSpaces()
//            }
//        }.start()
//    }

    private fun startTimer(remaining: Long) {
        timer = object : CountDownTimer(remaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                timerText.text = String.format("Time left: %02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                showTimeoutDialog()
                deleteBookingAndRestoreSpaces()
            }
        }.start()
    }


    private fun cancelBooking() {
        timer?.cancel()

        firestore.collection("pending_bookings").document(bookingDocId!!)
            .delete()
            .addOnSuccessListener {
                restoreSpaces()
                Toast.makeText(this, "Booking canceled", Toast.LENGTH_SHORT).show()
                finish() // return to booking page
            }
    }

    private fun deleteBookingAndRestoreSpaces() {
        firestore.collection("pending_bookings").document(bookingDocId!!)
            .delete()
            .addOnSuccessListener {
                restoreSpaces()
            }
    }

    private fun restoreSpaces() {
        firestore.collection("parking_lots")
            .whereEqualTo("lotNumber", lotNumber)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val doc = result.documents[0]
                    val currentSpaces = doc.getLong("totalSpaces") ?: 0L
                    val updatedSpaces = currentSpaces + selectedSpaces
                    firestore.collection("parking_lots")
                        .document(doc.id)
                        .update("totalSpaces", updatedSpaces)
                }
            }
    }

    private fun showTimeoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Booking Timed Out")
            .setMessage("5 minutes have passed. Your booking has been canceled.")
            .setCancelable(false)
            .setPositiveButton("Go to Home") { _, _ ->
                goToHomePage()
            }
            .show()
    }

    private fun goToHomePage() {
        val intent = Intent(this, ClientHomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
