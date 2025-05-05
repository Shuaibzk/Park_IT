package com.example.parkinglotbookingsystem

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Tasks

data class OwnerBooking(
    val lotNumber: String,
    val clientEmail: String,
    val date: String,
    val startTime: String,
    val spacesBooked: Int,
    val status: String
)

class OwnerBookingHistoryActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val bookingList = mutableListOf<OwnerBooking>()
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: OwnerBookingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)

            addView(TextView(this@OwnerBookingHistoryActivity).apply {
                text = "Booking History"
                textSize = 20f
                setTextColor(resources.getColor(android.R.color.black))
                setPadding(0, 0, 0, 16)
            })

            recycler = RecyclerView(this@OwnerBookingHistoryActivity).apply {
                layoutManager = LinearLayoutManager(this@OwnerBookingHistoryActivity)
            }

            addView(recycler)
        }

        setContentView(layout)
        adapter = OwnerBookingAdapter(bookingList)
        recycler.adapter = adapter

        fetchBookings()
    }

    private fun fetchBookings() {
        val ownerId = auth.currentUser?.uid ?: return

        db.collection("pending_bookings")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { snapshot ->
                val clientIds = snapshot.documents.mapNotNull { it.getString("clientId") }.toSet()
                val clientMap = mutableMapOf<String, String>()
                val tasks = clientIds.map { clientId ->
                    db.collection("users").document(clientId).get()
                        .addOnSuccessListener { doc ->
                            clientMap[clientId] = doc.getString("email") ?: "Unknown"
                        }
                }

                Tasks.whenAllSuccess<Void>(tasks).addOnSuccessListener {
                    bookingList.clear()
                    for (doc in snapshot) {
                        val lot = doc.getString("lotNumber") ?: continue
                        val clientId = doc.getString("clientId") ?: continue
                        val email = clientMap[clientId] ?: "Unknown"
                        val date = doc.getString("date") ?: ""
                        val startTime = doc.getString("startTime") ?: ""
                        val spaces = doc.getLong("spacesBooked")?.toInt() ?: 0
                        val status = doc.getString("status") ?: ""

                        bookingList.add(OwnerBooking(lot, email, date, startTime, spaces, status))
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }
}

class OwnerBookingAdapter(private val list: List<OwnerBooking>) :
    RecyclerView.Adapter<OwnerBookingAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text = view.findViewById<TextView>(R.id.tvBookingItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_owner_booking, parent, false)
        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = list[position]
        holder.text.text =
            "Lot: ${booking.lotNumber}\nClient: ${booking.clientEmail}\nDate: ${booking.date}\nTime: ${booking.startTime}\nSpaces: ${booking.spacesBooked}\nStatus: ${booking.status}"
    }

    override fun getItemCount(): Int = list.size
}
