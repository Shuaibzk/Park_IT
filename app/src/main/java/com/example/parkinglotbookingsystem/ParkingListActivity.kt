package com.example.parkinglotbookingsystem
// list view of parking lots for clients
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ParkingListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ParkingLotAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private var selectedDistrict: String? = null
    private var selectedDate: String? = null
    private var parkingLots = mutableListOf<ParkingLot>()




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parking_list)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        selectedDistrict = intent.getStringExtra("district")
        selectedDate = intent.getStringExtra("date")

        if (selectedDistrict == null || selectedDate == null) {
            Toast.makeText(this, "Missing filter data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadParkingLots()
    }

    private fun loadParkingLots() {
        Log.d("ParkingListActivity", "Loading lots for district: $selectedDistrict")

        firestore.collection("parking_lots")
            .whereEqualTo("district", selectedDistrict)
            .get()
            .addOnSuccessListener { result ->
                parkingLots.clear()
                Log.d("ParkingListActivity", "Got ${result.documents.size} lots from Firestore")

                var lotsProcessed = 0

                for (doc in result.documents) {
                    val lotNumber = doc.getString("parkingLotNumber") ?: continue
                    val pricePerHour = doc.getDouble("pricePerHour") ?: continue
                    val description = doc.getString("description") ?: "No description"
                    val totalSpaces = doc.getLong("totalSpaces")?.toInt() ?: continue
                    val latitude = doc.getDouble("latitude") ?: continue
                    val longitude = doc.getDouble("longitude") ?: continue
                    val ownerId = doc.getString("ownerId") ?: continue

                    val parkingLot = ParkingLot(
                        lotNumber, pricePerHour, description, totalSpaces,
                        latitude, longitude, ownerId
                    )

                    // Fetch review for this parking lot
                    firestore.collection("reviews")
                        .whereEqualTo("lotNumber", lotNumber)
                        .get()
                        .addOnSuccessListener { reviewsSnapshot ->
                            val ratings = reviewsSnapshot.documents.mapNotNull { it.getDouble("rating") }
                            val reviews = reviewsSnapshot.documents.mapNotNull { it.getString("review") }

                            if (ratings.isNotEmpty()) {
                                val avg = ratings.average()
                                parkingLot.averageRating = String.format("%.1f", avg).toDouble()
                            }

                            if (reviews.isNotEmpty()) {
                                parkingLot.recentReview = reviews.last()
                            }

                            parkingLots.add(parkingLot)
                            lotsProcessed++
                            if (lotsProcessed == result.documents.size) {
                                fetchAvailabilityAndDisplay()
                            }
                        }
                        .addOnFailureListener {
                            // Even if fetching reviews fails, still add the lot
                            parkingLots.add(parkingLot)
                            lotsProcessed++
                            if (lotsProcessed == result.documents.size) {
                                fetchAvailabilityAndDisplay()
                            }
                        }
                }
            }
            .addOnFailureListener {
                Log.e("ParkingListActivity", "Failed to load parking lots", it)
                Toast.makeText(this, "Failed to load parking lots", Toast.LENGTH_SHORT).show()
            }
    }



    private fun fetchAvailabilityAndDisplay() {

        Log.d("ParkingListActivity", "Starting availability checks for ${parkingLots.size} lots")


        if (parkingLots.isEmpty()) {
            Toast.makeText(this, "No parking lots found for this district.", Toast.LENGTH_LONG).show()
            return
        }

        var loadedCount = 0
        for (lot in parkingLots) {
            firestore.collection("pending_bookings")
                .whereEqualTo("lotNumber", lot.lotNumber)
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener { result ->
                    var occupied = 0
                    for (doc in result.documents) {
                        occupied += doc.getLong("spacesBooked")?.toInt() ?: 0
                    }

                    lot.availableSpaces = (lot.totalSpaces - occupied).coerceAtLeast(0)
                }
                .addOnCompleteListener {
                    loadedCount++
                    if (loadedCount == parkingLots.size) {
                        Log.d("ParkingListActivity", "Setting adapter with ${parkingLots.size} lots")
                        adapter = ParkingLotAdapter(parkingLots)
                        recyclerView.adapter = adapter
                    }

                }
        }
    }



    data class ParkingLot(
        val lotNumber: String,
        val pricePerHour: Double,
        val description: String,
        val totalSpaces: Int,
        val latitude: Double,
        val longitude: Double,
        val ownerId: String,
        var availableSpaces: Int = 0,
        var averageRating: Double = 0.0,
        var recentReview: String = ""
    )

    inner class ParkingLotAdapter(private val lots: List<ParkingLot>) :
        RecyclerView.Adapter<ParkingLotAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val lotNumberText: TextView = itemView.findViewById(R.id.lotNumberText)
            val priceText: TextView = itemView.findViewById(R.id.priceText)
            val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)
            val availableSpacesText: TextView = itemView.findViewById(R.id.availableSpacesText)
            val ratingText: TextView = itemView.findViewById(R.id.ratingText)       // ← Add this
            val reviewText: TextView = itemView.findViewById(R.id.reviewText)
            val bookNowBtn: Button = itemView.findViewById(R.id.bookNowBtn)
            val mapBtn: Button = itemView.findViewById(R.id.mapBtn)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_parking_lot, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val lot = lots[position]
            holder.lotNumberText.text = "Lot: ${lot.lotNumber}"
            holder.priceText.text = "Price: ${lot.pricePerHour} BDT/hr"
            holder.descriptionText.text = lot.description
            holder.availableSpacesText.text = "Available: ${lot.availableSpaces}"
            holder.ratingText.text = "Rating: ${lot.averageRating}"
            holder.reviewText.text = "Recent Review: ${lot.recentReview}"

            holder.bookNowBtn.setOnClickListener {
                val intent = Intent(this@ParkingListActivity, BookingActivity::class.java).apply {
                    putExtra("lotNumber", lot.lotNumber)
                    putExtra("pricePerHour", lot.pricePerHour)
                    putExtra("totalSpaces", lot.availableSpaces)
                    putExtra("ownerId", lot.ownerId)
                }
                startActivity(intent)
            }

            holder.mapBtn.setOnClickListener {
                val intent = Intent(this@ParkingListActivity, ClientMapActivity::class.java).apply {
                    putExtra("highlightLat", lot.latitude)
                    putExtra("highlightLon", lot.longitude)
                    putExtra("highlightLotNumber", lot.lotNumber)
                }
                startActivity(intent)
            }
        }

        override fun getItemCount(): Int = lots.size
    }
}
