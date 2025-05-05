package com.example.parkinglotbookingsystem

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class ClientFilterActivity : AppCompatActivity() {

    private lateinit var districtSpinner: Spinner
    private lateinit var dateBtn: Button
    private lateinit var searchBtn: Button

    private var selectedDistrict: String? = null
    private var selectedDate: String? = null // formatted as dd/MM/yyyy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_filter)

        districtSpinner = findViewById(R.id.districtSpinner)
        dateBtn = findViewById(R.id.dateBtn)
        searchBtn = findViewById(R.id.searchBtn)

        setupDistrictSpinner()

        dateBtn.setOnClickListener {
            val cal = Calendar.getInstance()
            val datePicker = DatePickerDialog(this, { _, year, month, day ->
                cal.set(year, month, day)
                selectedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
                dateBtn.text = "Date: $selectedDate"
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }

        searchBtn.setOnClickListener {
            selectedDistrict = districtSpinner.selectedItem?.toString()

            if (selectedDistrict.isNullOrBlank() || selectedDate == null) {
                Toast.makeText(this, "Please select both district and date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Pass selected filters to the result list activity
            val intent = Intent(this, ParkingListActivity::class.java).apply {
                putExtra("district", selectedDistrict)
                putExtra("date", selectedDate)
            }
            startActivity(intent)
        }
    }

    private fun setupDistrictSpinner() {
        val districts = listOf(
            "Dhaka", "Chattogram", "Khulna", "Sylhet",
            "Barisal", "Rajshahi", "Rangpur", "Mymensingh"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, districts)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        districtSpinner.adapter = adapter
    }
}
