package com.example.parkinglotbookingsystem

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ClientReportActivity : AppCompatActivity() {
    private lateinit var reportType: String
    private lateinit var firestore: FirebaseFirestore
    private lateinit var clientId: String

    private lateinit var ownerList: List<Pair<String, String>>
    private lateinit var lotList: List<Pair<String, String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_report)

        firestore = FirebaseFirestore.getInstance()
        clientId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
        reportType = intent.getStringExtra("reportType") ?: "general"

        if (reportType == "general") {
            setupGeneralReportUI()
        } else {
            setupLotOwnerReportUI()
        }

        findViewById<Button>(R.id.submitButton).setOnClickListener {
            submitReport()
        }
    }

    private fun setupGeneralReportUI() {
        findViewById<LinearLayout>(R.id.ownerLayout).visibility = View.GONE
        findViewById<TextView>(R.id.reportTitle).text = "General Report"
    }

    private fun setupLotOwnerReportUI() {
        val ownerLayout = findViewById<LinearLayout>(R.id.ownerLayout)
        val ownerSpinner = findViewById<Spinner>(R.id.ownerSpinner)
        val lotSpinner = findViewById<Spinner>(R.id.lotSpinner)

        ownerLayout.visibility = View.VISIBLE
        findViewById<TextView>(R.id.reportTitle).text = "Report Lot Owner"

        firestore.collection("users").whereEqualTo("role", "owner").get().addOnSuccessListener { snapshot ->
            ownerList = snapshot.documents.map { it.id to (it.getString("name") ?: "Unnamed Owner") }
            ownerSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ownerList.map { it.second })

            ownerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selectedOwnerId = ownerList[position].first
                    firestore.collection("parking_lots")
                        .whereEqualTo("ownerId", selectedOwnerId)
                        .get().addOnSuccessListener { lots ->
                            lotList = lots.documents.map {
                                it.id to "${it.getString("parkingLotNumber")} - ${it.getString("district")}"
                            }
                            lotSpinner.adapter = ArrayAdapter(this@ClientReportActivity, android.R.layout.simple_spinner_dropdown_item, lotList.map { it.second })
                        }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
    }

    private fun submitReport() {
        val message = findViewById<EditText>(R.id.messageEditText).text.toString().trim()
        if (message.isEmpty()) {
            Toast.makeText(this, "Please write a message", Toast.LENGTH_SHORT).show()
            return
        }

        val reportData = hashMapOf(
            "clientId" to clientId,
            "message" to message,
            "reportType" to reportType,
            "timestamp" to FieldValue.serverTimestamp()
        )

        if (reportType == "owner") {
            val ownerSpinner = findViewById<Spinner>(R.id.ownerSpinner)
            val lotSpinner = findViewById<Spinner>(R.id.lotSpinner)

            val selectedOwnerId = ownerList[ownerSpinner.selectedItemPosition].first
            val selectedLotId = lotList[lotSpinner.selectedItemPosition].first

            reportData["ownerId"] = selectedOwnerId
            reportData["lotId"] = selectedLotId
        }

        firestore.collection("report_client_reports")
            .add(reportData)

            .addOnSuccessListener {
                Toast.makeText(this, "Report submitted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Submission failed", Toast.LENGTH_SHORT).show()
            }
    }
}
