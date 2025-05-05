package com.example.parkinglotbookingsystem

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class ApproveParkingLotsActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var pendingLotsContainer: LinearLayout
    private lateinit var noRequestsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_approve_parking_lots)

        firestore = FirebaseFirestore.getInstance()
        pendingLotsContainer = findViewById(R.id.pendingLotsContainer)

        // Dynamically add a "No requests" TextView
        noRequestsText = TextView(this).apply {
            text = "No pending parking lot approval requests."
            textSize = 18f
            setTextColor(resources.getColor(android.R.color.darker_gray))
            visibility = View.GONE
            setPadding(0, 50, 0, 0)
            gravity = android.view.Gravity.CENTER
        }
        pendingLotsContainer.addView(noRequestsText)

        loadPendingLots()
    }

    private fun loadPendingLots() {
        firestore.collection("pending_parking_lots").get()
            .addOnSuccessListener { result ->
                pendingLotsContainer.removeAllViews()

                if (result.isEmpty) {
                    noRequestsText.visibility = View.VISIBLE
                    pendingLotsContainer.addView(noRequestsText)
                } else {
                    noRequestsText.visibility = View.GONE

                    var requestNumber = 1

                    for (doc in result) {
                        val lotId = doc.id
                        val data = doc.data

                        val lotView = layoutInflater.inflate(R.layout.item_pending_lot, null)

                        val requestNumberText = lotView.findViewById<TextView>(R.id.requestNumber)
                        val ownerInfoText = lotView.findViewById<TextView>(R.id.ownerInfo)
                        val lotTitleText = lotView.findViewById<TextView>(R.id.lotTitle)
                        val lotInfoText = lotView.findViewById<TextView>(R.id.lotInfo)
                        val approveBtn = lotView.findViewById<Button>(R.id.approveBtn)
                        val disapproveBtn = lotView.findViewById<Button>(R.id.disapproveBtn)

                        requestNumberText.text = "Request #$requestNumber"

                        val ownerId = data["ownerId"] as? String ?: "Unknown"

                        // Fetch Owner Name
                        firestore.collection("users").document(ownerId).get()
                            .addOnSuccessListener { userDoc ->
                                val ownerName = userDoc.getString("name") ?: "Unknown"
                                ownerInfoText.text = "Owner: $ownerName (ID: $ownerId)"
                            }
                            .addOnFailureListener {
                                ownerInfoText.text = "Owner ID: $ownerId"
                            }

                        lotTitleText.text = "Parking Lot: ${data["parkingLotNumber"] ?: "N/A"}"
                        lotInfoText.text = """
                        Location: (${data["latitude"]}, ${data["longitude"]})
                        Price: ${data["pricePerHour"]} per hour
                        Spaces: ${data["totalSpaces"]}
                        Description: ${data["description"] ?: "N/A"}
                    """.trimIndent()

                        approveBtn.setOnClickListener {
                            approveLot(doc.id, data)
                        }

                        disapproveBtn.setOnClickListener {
                            disapproveLot(doc.id)
                        }

                        pendingLotsContainer.addView(lotView)
                        requestNumber++
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load pending requests", Toast.LENGTH_SHORT).show()
            }
    }

    private fun approveLot(docId: String, data: Map<String, Any?>) {
        val cleanData = HashMap(data)
        cleanData["createdAt"] = Timestamp.now()

        firestore.collection("parking_lots").add(cleanData)
            .addOnSuccessListener {
                firestore.collection("pending_parking_lots").document(docId).delete()
                Toast.makeText(this, "Parking lot approved!", Toast.LENGTH_SHORT).show()
                loadPendingLots()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Approval failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun disapproveLot(docId: String) {
        firestore.collection("pending_parking_lots").document(docId).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Request disapproved", Toast.LENGTH_SHORT).show()
                loadPendingLots()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to disapprove", Toast.LENGTH_SHORT).show()
            }
    }
}