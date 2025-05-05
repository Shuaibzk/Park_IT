package com.example.parkinglotbookingsystem
// admin - manage parking lot 1
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

data class ParkingLotDisplay(
    val id: String,
    val lotNumber: String,
    val ownerId: String,
    val ownerEmail: String,
    val district: String,
    val totalSpaces: Int,
    val avgRating: Double,
    val status: String, // Approved, Pending, Banned
    val isPending: Boolean
)

class ManageParkingLotsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val allLots = mutableListOf<ParkingLotDisplay>()
    private lateinit var adapter: ParkingLotAdapter
    private lateinit var recycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ManageParkingLotsActivity)
        }
        setContentView(recycler)
        adapter = ParkingLotAdapter(allLots,
            onDelete = { deleteLot(it) },
            onCheckBookings = { checkBookings(it) }
        )
        recycler.adapter = adapter
        fetchLots()
    }

    private fun fetchLots() {
        allLots.clear()

        // Fetch approved + banned lots
        db.collection("parking_lots").get().addOnSuccessListener { lotSnap ->
            val lots = lotSnap.documents.map { doc ->
                val ownerId = doc.getString("ownerId") ?: ""
                val lotNumber = doc.getString("parkingLotNumber") ?: ""
                val district = doc.getString("district") ?: ""
                val totalSpaces = doc.getLong("totalSpaces")?.toInt() ?: 0
                val banned = doc.getBoolean("banned") ?: false
                val status = if (banned) "Banned" else "Approved"
                val id = doc.id

                Pair(id, Triple(ownerId, lotNumber, Triple(district, totalSpaces, status)))
            }

            db.collection("users").get().addOnSuccessListener { userSnap ->
                val userMap = userSnap.documents.associateBy({ it.id }, { it.getString("email") ?: "" })

                db.collection("reviews").get().addOnSuccessListener { reviewSnap ->
                    val reviewGroups = reviewSnap.documents.groupBy {
                        it.getString("lotNumber") ?: ""
                    }.mapValues { entry ->
                        val ratings = entry.value.mapNotNull { it.getDouble("rating") }
                        if (ratings.isNotEmpty()) ratings.average() else 0.0
                    }

                    for ((id, triple) in lots) {
                        val (ownerId, lotNumber, detail) = triple
                        val (district, totalSpaces, status) = detail
                        val email = userMap[ownerId] ?: "Unknown"
                        val rating = reviewGroups[lotNumber] ?: 0.0

                        allLots.add(
                            ParkingLotDisplay(id, lotNumber, ownerId, email, district, totalSpaces, rating, status, isPending = false)
                        )
                    }

                    // Fetch pending lots
                    db.collection("pending_parking_lots").get().addOnSuccessListener { pendSnap ->
                        for (doc in pendSnap) {
                            val ownerId = doc.getString("ownerId") ?: ""
                            val lotNumber = doc.getString("parkingLotNumber") ?: ""
                            val district = doc.getString("district") ?: ""
                            val totalSpaces = doc.getLong("totalSpaces")?.toInt() ?: 0
                            val id = doc.id
                            val email = userMap[ownerId] ?: "Unknown"

                            allLots.add(
                                ParkingLotDisplay(id, lotNumber, ownerId, email, district, totalSpaces, 0.0, "Pending", isPending = true)
                            )
                        }

                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun deleteLot(lot: ParkingLotDisplay) {
        val collection = if (lot.isPending) "pending_parking_lots" else "parking_lots"
        db.collection(collection).document(lot.id).delete().addOnSuccessListener {
            // delete related bookings
            db.collection("pending_bookings").whereEqualTo("lotNumber", lot.lotNumber)
                .get().addOnSuccessListener { bookings ->
                    val batch = db.batch()
                    for (b in bookings) {
                        batch.delete(b.reference)
                    }
                    batch.commit()
                }

            Toast.makeText(this, "Deleted lot ${lot.lotNumber}", Toast.LENGTH_SHORT).show()
            allLots.remove(lot)
            adapter.notifyDataSetChanged()
        }
    }

    private fun checkBookings(lot: ParkingLotDisplay) {
        val intent = Intent(this, CheckBookingsActivity::class.java)
        intent.putExtra("lotNumber", lot.lotNumber)
        startActivity(intent)
    }
}

class ParkingLotAdapter(
    private val list: List<ParkingLotDisplay>,
    private val onDelete: (ParkingLotDisplay) -> Unit,
    private val onCheckBookings: (ParkingLotDisplay) -> Unit
) : RecyclerView.Adapter<ParkingLotAdapter.LotViewHolder>() {

    inner class LotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val info = view.findViewById<TextView>(R.id.tvInfo)
        val status = view.findViewById<TextView>(R.id.tvStatus)
        val btnDelete = view.findViewById<Button>(R.id.btnDelete)
        val btnCheck = view.findViewById<Button>(R.id.btnCheckBookings)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LotViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.lot_item_admin, parent, false)
        return LotViewHolder(layout)
    }

    override fun onBindViewHolder(holder: LotViewHolder, position: Int) {
        val lot = list[position]
        val statusColor = when (lot.status) {
            "Approved" -> Color.parseColor("#4CAF50")
            "Pending" -> Color.parseColor("#FFC107")
            "Banned" -> Color.parseColor("#F44336")
            else -> Color.GRAY
        }

        holder.info.text = "Lot: ${lot.lotNumber}\nOwner: ${lot.ownerEmail}\nDistrict: ${lot.district}\nSpaces: ${lot.totalSpaces}\nRating: ${"%.1f".format(lot.avgRating)}"
        holder.status.text = lot.status
        holder.status.setTextColor(statusColor)
        holder.btnDelete.setOnClickListener { onDelete(lot) }
        holder.btnCheck.setOnClickListener { onCheckBookings(lot) }
    }

    override fun getItemCount(): Int = list.size
}
