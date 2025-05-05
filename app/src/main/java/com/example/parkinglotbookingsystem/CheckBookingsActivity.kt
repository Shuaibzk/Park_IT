package com.example.parkinglotbookingsystem
// admin - manage parking 2


import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

data class Booking(
    val id: String,
    val clientId: String,
    val date: String,
    val startTime: String,
    val durationHours: Int,
    val spacesBooked: Int
)

class CheckBookingsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val bookings = mutableListOf<Booking>()
    private lateinit var adapter: BookingAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var lotNumber: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@CheckBookingsActivity)
        }
        setContentView(recycler)

        adapter = BookingAdapter(bookings) { booking -> cancelBooking(booking) }
        recycler.adapter = adapter

        lotNumber = intent.getStringExtra("lotNumber") ?: ""
        fetchBookings()
    }

    private fun fetchBookings() {
        bookings.clear()
        db.collection("pending_bookings").whereEqualTo("lotNumber", lotNumber).get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot) {
                    val id = doc.id
                    val clientId = doc.getString("clientId") ?: ""
                    val date = doc.getString("date") ?: ""
                    val startTime = doc.getString("startTime") ?: ""
                    val duration = doc.getLong("durationHours")?.toInt() ?: 0
                    val spaces = doc.getLong("spacesBooked")?.toInt() ?: 0

                    bookings.add(Booking(id, clientId, date, startTime, duration, spaces))
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun cancelBooking(booking: Booking) {
        db.collection("pending_bookings").document(booking.id).delete().addOnSuccessListener {
            Toast.makeText(this, "Booking cancelled", Toast.LENGTH_SHORT).show()
            bookings.remove(booking)
            adapter.notifyDataSetChanged()
        }
    }
}

class BookingAdapter(
    private val list: List<Booking>,
    private val onCancel: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    inner class BookingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val info = view.findViewById<TextView>(R.id.tvBookingInfo)
        val cancel = view.findViewById<Button>(R.id.btnCancel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.booking_item_admin, parent, false)
        return BookingViewHolder(layout)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = list[position]
        holder.info.text = "Client: ${booking.clientId}\nDate: ${booking.date}\nTime: ${booking.startTime}\nSpaces: ${booking.spacesBooked}"
        holder.cancel.setOnClickListener { onCancel(booking) }
    }

    override fun getItemCount(): Int = list.size
}
