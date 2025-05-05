// client "my bookings" layout

package com.example.parkinglotbookingsystem

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MyBookingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val bookings = mutableListOf<Booking>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_bookings)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadBookings()
    }

    // reload booking when activity is resumed
    override fun onResume() {
        super.onResume()
        loadBookings()
    }

    private fun loadBookings() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("pending_bookings")
            .whereEqualTo("clientId", userId)
            .get()
            .addOnSuccessListener { result ->
                bookings.clear()
                for (doc in result.documents) {
                    val booking = Booking(
                        doc.id,
                        doc.getString("lotNumber") ?: "",
                        doc.getString("date") ?: "",
                        doc.getString("startTime") ?: "",
                        doc.getString("endTime") ?: "",
                        doc.getDouble("totalPrice") ?: 0.0,
                        doc.getString("status") ?: "pending",
                        doc.getLong("timestamp") ?: 0L,
                        doc.getString("ownerId") ?: ""
                    )
                    bookings.add(booking)
                }
                recyclerView.adapter = BookingAdapter(bookings)
            }
    }

    data class Booking(
        val id: String,
        val lotNumber: String,
        val date: String,
        val startTime: String,
        val endTime: String,
        val totalPrice: Double,
        val status: String,
        val timestamp: Long,
        val ownerId: String
    )

    inner class BookingAdapter(private val items: List<Booking>) : RecyclerView.Adapter<BookingAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val dateTimeText: TextView = view.findViewById(R.id.dateTimeText)
            val lotText: TextView = view.findViewById(R.id.lotText)
            val priceText: TextView = view.findViewById(R.id.priceText)
            val statusText: TextView = view.findViewById(R.id.statusText)
            val cancelBtn: Button = view.findViewById(R.id.cancelBookingBtn)
            val confirmBtn: Button = view.findViewById(R.id.confirmBookingBtn)
            val giveReviewBtn: Button = view.findViewById(R.id.giveReviewBtn)

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booking, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val booking = items[position]
            holder.dateTimeText.text = "${booking.date} at ${booking.startTime}"  // Bold in XML
            holder.lotText.text = "Lot: ${booking.lotNumber}"
            holder.priceText.text = "Total: ${"%.2f".format(booking.totalPrice)} BDT"
            holder.statusText.text = "Status: ${booking.status.uppercase()}"

            // Check time difference
            val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
            val bookingStart = sdf.parse("${booking.date} ${booking.startTime}")
            val timeDiff = bookingStart?.time?.minus(System.currentTimeMillis()) ?: 0L

            val canCancel = timeDiff > 12 * 60 * 60 * 1000
            val isExpired = timeDiff <= 0
            val within5Min = System.currentTimeMillis() - booking.timestamp <= 5 * 60 * 1000

            holder.cancelBtn.isEnabled = canCancel
            holder.cancelBtn.alpha = if (canCancel) 1f else 0.5f
            holder.cancelBtn.setOnClickListener {
                firestore.collection("pending_bookings").document(booking.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this@MyBookingsActivity, "Booking canceled", Toast.LENGTH_SHORT).show()
                        loadBookings()
                    }
            }

            holder.confirmBtn.visibility = if (booking.status == "pending" && within5Min && !isExpired) View.VISIBLE else View.GONE
            holder.confirmBtn.setOnClickListener {
                val intent = Intent(this@MyBookingsActivity, PaymentActivity::class.java).apply {
                    putExtra("bookingDocId", booking.id)
                    putExtra("lotNumber", booking.lotNumber)
                    putExtra("spacesBooked", 1)  // Replace with actual if needed
                }
                startActivity(intent)
            }

            // give rating button logic
            holder.giveReviewBtn.visibility = View.GONE

            if (booking.status == "confirmed") {
                val userId = auth.currentUser?.uid ?: return
                firestore.collection("reviews")
                    .whereEqualTo("clientId", userId)
                    .whereEqualTo("lotNumber", booking.lotNumber)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.isEmpty) {
                            holder.giveReviewBtn.visibility = View.VISIBLE
                            holder.giveReviewBtn.setOnClickListener {
                                val intent = Intent(this@MyBookingsActivity, ReviewActivity::class.java).apply {
                                    putExtra("lotNumber", booking.lotNumber)
                                    putExtra("ownerId", "")
                                }
                                startActivity(intent)
                            }
                        }
                    }
            }

        }

        override fun getItemCount(): Int = items.size


    }
}
