package com.example.parkinglotbookingsystem

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.QuerySnapshot



class OwnerReportActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var ownerId: String
    private lateinit var reportType: String

    private lateinit var clientList: List<Pair<String, String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owner_report)

        firestore = FirebaseFirestore.getInstance()
        ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
        reportType = intent.getStringExtra("reportType") ?: "general"

        val reportTitle = findViewById<TextView>(R.id.reportTitle)
        val clientLayout = findViewById<LinearLayout>(R.id.clientLayout)

        if (reportType == "general") {
            reportTitle.text = "General Report"
            clientLayout.visibility = View.GONE
        } else {
            reportTitle.text = "Report Client"
            clientLayout.visibility = View.VISIBLE
            setupClientSpinner()
        }

        findViewById<Button>(R.id.submitButton).setOnClickListener {
            submitReport()
        }
    }

    private fun setupClientSpinner() {
        val clientSpinner = findViewById<Spinner>(R.id.clientSpinner)
        val allClientIds = mutableSetOf<String>()

        val fetchConfirmed = firestore.collection("confirmed_bookings")
            .whereEqualTo("ownerId", ownerId)
            .get()

        val fetchPending = firestore.collection("pending_bookings")
            .whereEqualTo("ownerId", ownerId)
            .get()

        // Run both queries in parallel
        Tasks.whenAllSuccess<QuerySnapshot>(fetchConfirmed, fetchPending)
            .addOnSuccessListener { snapshots ->
                snapshots.forEach { snapshot ->
                    snapshot.documents.forEach { doc ->
                        val clientId = doc.getString("clientId")
                        if (!clientId.isNullOrEmpty()) {
                            allClientIds.add(clientId)
                        }
                    }
                }

                if (allClientIds.isEmpty()) {
                    Toast.makeText(this, "No clients found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                firestore.collection("users")
                    .whereIn(FieldPath.documentId(), allClientIds.toList())
                    .get()
                    .addOnSuccessListener { users ->
                        clientList = users.documents.map { it.id to (it.getString("name") ?: "Unnamed Client") }
                        clientSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, clientList.map { it.second })
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load clients", Toast.LENGTH_SHORT).show()
            }
    }



    private fun submitReport() {
        val message = findViewById<EditText>(R.id.messageEditText).text.toString().trim()
        if (message.isEmpty()) {
            Toast.makeText(this, "Please write a message", Toast.LENGTH_SHORT).show()
            return
        }

        val reportData = hashMapOf(
            "ownerId" to ownerId,
            "message" to message,
            "reportType" to reportType,
            "timestamp" to FieldValue.serverTimestamp()
        )

        if (reportType == "client") {
            val clientSpinner = findViewById<Spinner>(R.id.clientSpinner)
            val selectedClientId = clientList[clientSpinner.selectedItemPosition].first
            reportData["clientId"] = selectedClientId
        }

        firestore.collection("report_owner_reports")
            .add(reportData)
            .addOnSuccessListener {
                Toast.makeText(this, "Report submitted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to submit report", Toast.LENGTH_SHORT).show()
            }
    }
}
