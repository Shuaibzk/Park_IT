package com.example.parkinglotbookingsystem
// owner viewing their parking lots
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ViewMyParkingLotsActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var myLotsContainer: LinearLayout
    private lateinit var noLotsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_my_parking_lots)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        myLotsContainer = findViewById(R.id.myLotsContainer)

        // TextView for showing "No parking lots" message
        noLotsText = TextView(this).apply {
            text = "You have not registered any parking lots yet."
            textSize = 18f
            setTextColor(resources.getColor(android.R.color.darker_gray))
            visibility = View.GONE
            setPadding(0, 50, 0, 0)
            gravity = android.view.Gravity.CENTER
        }
        myLotsContainer.addView(noLotsText)

        loadMyParkingLots()
    }

//    private fun loadMyParkingLots() {
//        val ownerId = auth.currentUser?.uid
//
//        if (ownerId == null) {
//            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
//            finish()
//            return
//        }
//
//        firestore.collection("parking_lots")
//            .whereEqualTo("ownerId", ownerId)
//            .get()
//            .addOnSuccessListener { result ->
//                myLotsContainer.removeAllViews()
//
//                if (result.isEmpty) {
//                    noLotsText.visibility = View.VISIBLE
//                    myLotsContainer.addView(noLotsText)
//                } else {
//                    noLotsText.visibility = View.GONE
//
//                    for (doc in result) {
//                        val data = doc.data
//                        val docId = doc.id   // ⬅️ Important for editing and deleting
//
//                        val lotView = layoutInflater.inflate(R.layout.item_my_parking_lot, null)
//
//                        val lotTitleText = lotView.findViewById<TextView>(R.id.lotTitle)
//                        val lotInfoText = lotView.findViewById<TextView>(R.id.lotInfo)
//                        val editBtn = lotView.findViewById<Button>(R.id.editBtn)
//                        val deleteBtn = lotView.findViewById<Button>(R.id.deleteBtn)
//
//                        lotTitleText.text = "Parking Lot: ${data["parkingLotNumber"] ?: "N/A"}"
//                        lotInfoText.text = """
//                        Price: ${data["pricePerHour"] ?: "N/A"} per hour
//                        Spaces: ${data["totalSpaces"] ?: "N/A"}
//                        Location: (${data["latitude"]}, ${data["longitude"]})
//                    """.trimIndent()
//
//                        // Edit Button
//                        editBtn.setOnClickListener {
//                            val intent = Intent(this, EditParkingLotActivity::class.java)
//                            intent.putExtra("lotId", docId) // Send lot ID
//                            startActivity(intent)
//                        }
//
//                        // Delete Button
//                        deleteBtn.setOnClickListener {
//                            firestore.collection("parking_lots").document(docId)
//                                .delete()
//                                .addOnSuccessListener {
//                                    Toast.makeText(this, "Parking lot deleted", Toast.LENGTH_SHORT).show()
//                                    loadMyParkingLots() // Refresh after delete
//                                }
//                                .addOnFailureListener {
//                                    Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
//                                }
//                        }
//
//                        myLotsContainer.addView(lotView)
//                    }
//                }
//            }
//            .addOnFailureListener {
//                Toast.makeText(this, "Failed to load parking lots", Toast.LENGTH_SHORT).show()
//            }
//    }

    private fun loadMyParkingLots() {
        val ownerId = auth.currentUser?.uid

        if (ownerId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        firestore.collection("parking_lots")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { result ->
                myLotsContainer.removeAllViews()

                if (result.isEmpty) {
                    noLotsText.visibility = View.VISIBLE
                    myLotsContainer.addView(noLotsText)
                } else {
                    noLotsText.visibility = View.GONE

                    for (doc in result) {
                        val data = doc.data
                        val docId = doc.id

                        val lotView = layoutInflater.inflate(R.layout.item_my_parking_lot, null)

                        val lotTitleText = lotView.findViewById<TextView>(R.id.lotTitle)
                        val lotInfoText = lotView.findViewById<TextView>(R.id.lotInfo)
                        val editBtn = lotView.findViewById<Button>(R.id.editBtn)
                        val deleteBtn = lotView.findViewById<Button>(R.id.deleteBtn)
                        val avgRatingText = lotView.findViewById<TextView>(R.id.averageRatingText)
                        val viewReviewsBtn = lotView.findViewById<Button>(R.id.viewReviewsBtn)

                        val lotNumber = data["parkingLotNumber"] as? String ?: "N/A"

                        lotTitleText.text = "Parking Lot: $lotNumber"
                        lotInfoText.text = """
                        Price: ${data["pricePerHour"] ?: "N/A"} per hour
                        Spaces: ${data["totalSpaces"] ?: "N/A"}
                        Location: (${data["latitude"]}, ${data["longitude"]})
                    """.trimIndent()

                        // 👉 Fetch average rating
                        firestore.collection("reviews")
                            .whereEqualTo("lotNumber", lotNumber)
                            .get()
                            .addOnSuccessListener { reviews ->
                                val ratings = reviews.mapNotNull { it.getDouble("rating") }
                                if (ratings.isNotEmpty()) {
                                    val avg = String.format("%.1f", ratings.average())
                                    avgRatingText.text = "Rating: $avg ★"
                                } else {
                                    avgRatingText.text = "Rating: N/A"
                                }
                            }
                            .addOnFailureListener {
                                avgRatingText.text = "Rating: N/A"
                            }

                        //  View reviews
                        viewReviewsBtn.setOnClickListener {
                            Log.d("ReviewBtn", "Clicked for lot: $lotNumber")
                            val intent = Intent(this, LotReviewsActivity::class.java)
                            intent.putExtra("lotNumber", lotNumber)
                            startActivity(intent)
                        }

                        //  Edit button
                        editBtn.setOnClickListener {
                            val intent = Intent(this, EditParkingLotActivity::class.java)
                            intent.putExtra("lotId", docId)
                            startActivity(intent)
                        }

                        //  Delete button
                        deleteBtn.setOnClickListener {
                            firestore.collection("parking_lots").document(docId)
                                .delete()
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Parking lot deleted", Toast.LENGTH_SHORT).show()
                                    loadMyParkingLots()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
                                }
                        }

                        myLotsContainer.addView(lotView)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load parking lots", Toast.LENGTH_SHORT).show()
            }
    }

}