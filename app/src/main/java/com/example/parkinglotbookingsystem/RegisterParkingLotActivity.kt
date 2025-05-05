package com.example.parkinglotbookingsystem

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterParkingLotActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_parking_lot)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val lotNumberInput = findViewById<EditText>(R.id.lotNumberInput)
        val latitudeInput = findViewById<EditText>(R.id.latitudeInput)
        val longitudeInput = findViewById<EditText>(R.id.longitudeInput)
        val priceInput = findViewById<EditText>(R.id.priceInput)
        val spaceInput = findViewById<EditText>(R.id.spaceInput)
        val descriptionInput = findViewById<EditText>(R.id.descriptionInput)
        val districtSpinner = findViewById<Spinner>(R.id.districtSpinner)
        val submitBtn = findViewById<Button>(R.id.submitParkingBtn)

        // 1. Populate the district spinner
        val districts = listOf("Dhaka", "Chattogram", "Sylhet", "Rajshahi", "Khulna", "Barishal", "Mymensingh", "Rangpur")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, districts)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        districtSpinner.adapter = adapter

        submitBtn.setOnClickListener {
            val lotNumber = lotNumberInput.text.toString().trim()
            val latitudeStr = latitudeInput.text.toString().trim()
            val longitudeStr = longitudeInput.text.toString().trim()
            val priceStr = priceInput.text.toString().trim()
            val spaceStr = spaceInput.text.toString().trim()
            val description = descriptionInput.text.toString().trim()
            val selectedDistrict = districtSpinner.selectedItem?.toString() ?: ""

            if (lotNumber.isEmpty() || latitudeStr.isEmpty() || longitudeStr.isEmpty()
                || priceStr.isEmpty() || spaceStr.isEmpty() || selectedDistrict.isEmpty()
            ) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val latitude = latitudeStr.toDoubleOrNull()
            val longitude = longitudeStr.toDoubleOrNull()
            val pricePerHour = priceStr.toDoubleOrNull()
            val totalSpaces = spaceStr.toIntOrNull()

            if (latitude == null || longitude == null || pricePerHour == null || totalSpaces == null) {
                Toast.makeText(this, "Please enter valid numeric values", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ownerId = auth.currentUser?.uid ?: run {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Add district to the parking lot data
            val lotData = hashMapOf(
                "parkingLotNumber" to lotNumber,
                "latitude" to latitude,
                "longitude" to longitude,
                "pricePerHour" to pricePerHour,
                "totalSpaces" to totalSpaces,
                "description" to description,
                "district" to selectedDistrict,
                "ownerId" to ownerId,
                "createdAt" to Timestamp.now()
            )

            firestore.collection("pending_parking_lots")
                .add(lotData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Parking lot registered successfully!", Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
