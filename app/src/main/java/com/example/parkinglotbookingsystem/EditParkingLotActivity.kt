package com.example.parkinglotbookingsystem

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class EditParkingLotActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    private lateinit var editLotNumberInput: EditText
    private lateinit var editLatitudeInput: EditText
    private lateinit var editLongitudeInput: EditText
    private lateinit var editPriceInput: EditText
    private lateinit var editSpacesInput: EditText
    private lateinit var editDescriptionInput: EditText
    private lateinit var updateLotBtn: Button
    private lateinit var goBackBtn: Button

    private var lotId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_parking_lot)

        firestore = FirebaseFirestore.getInstance()

        editLotNumberInput = findViewById(R.id.editLotNumberInput)
        editLatitudeInput = findViewById(R.id.editLatitudeInput)
        editLongitudeInput = findViewById(R.id.editLongitudeInput)
        editPriceInput = findViewById(R.id.editPriceInput)
        editSpacesInput = findViewById(R.id.editSpacesInput)
        editDescriptionInput = findViewById(R.id.editDescriptionInput)
        updateLotBtn = findViewById(R.id.updateLotBtn)
        goBackBtn = findViewById(R.id.goBackBtn)

        lotId = intent.getStringExtra("lotId")

        if (lotId == null) {
            Toast.makeText(this, "Parking lot not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadParkingLotData()

        updateLotBtn.setOnClickListener {
            updateParkingLot()
        }

        goBackBtn.setOnClickListener {
            finish() // Close Edit Page without saving
        }
    }

    private fun loadParkingLotData() {
        firestore.collection("parking_lots").document(lotId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    editLotNumberInput.setText(document.getString("parkingLotNumber") ?: "")
                    editLatitudeInput.setText(document.getDouble("latitude")?.toString() ?: "")
                    editLongitudeInput.setText(document.getDouble("longitude")?.toString() ?: "")
                    editPriceInput.setText(document.getDouble("pricePerHour")?.toString() ?: "")
                    editSpacesInput.setText(document.getLong("totalSpaces")?.toString() ?: "")
                    editDescriptionInput.setText(document.getString("description") ?: "")
                } else {
                    Toast.makeText(this, "Parking lot not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun updateParkingLot() {
        val lotNumber = editLotNumberInput.text.toString().trim()
        val latitude = editLatitudeInput.text.toString().trim()
        val longitude = editLongitudeInput.text.toString().trim()
        val price = editPriceInput.text.toString().trim()
        val spaces = editSpacesInput.text.toString().trim()
        val description = editDescriptionInput.text.toString().trim()

        if (lotNumber.isEmpty() || latitude.isEmpty() || longitude.isEmpty() || price.isEmpty() || spaces.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedData = mapOf(
            "parkingLotNumber" to lotNumber,
            "latitude" to latitude.toDouble(),
            "longitude" to longitude.toDouble(),
            "pricePerHour" to price.toDouble(),
            "totalSpaces" to spaces.toInt(),
            "description" to description
        )

        firestore.collection("parking_lots").document(lotId!!)
            .update(updatedData)
            .addOnSuccessListener {
                Toast.makeText(this, "Parking lot updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
            }
    }
}