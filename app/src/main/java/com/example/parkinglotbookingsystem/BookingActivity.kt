// client Book parking lot -> booking layout
package com.example.parkinglotbookingsystem

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class BookingActivity : AppCompatActivity() {

    private lateinit var lotNumberText: TextView
    private lateinit var dateText: TextView
    private lateinit var startTimeText: TextView
    private lateinit var durationSpinner: Spinner
    private lateinit var spaceSpinner: Spinner
    private lateinit var makePaymentBtn: Button
    private lateinit var goBackBtn: Button

    private var bookingDate: String? = null
    private var startTime: String? = null
    private var selectedSpaces = 1
    private var totalSpaces = 1
    private var lotNumber = ""
    private var pricePerHour = 0.0

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var ownerId = ""

    private var bookingDocId: String? = null
    private var timer: CountDownTimer? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        lotNumberText = findViewById(R.id.lotNumberText)
        dateText = findViewById(R.id.dateText)
        startTimeText = findViewById(R.id.startTimeText)
        durationSpinner = findViewById(R.id.durationSpinner)
        spaceSpinner = findViewById(R.id.spaceSpinner)
        makePaymentBtn = findViewById(R.id.makePaymentBtn)

        goBackBtn = findViewById(R.id.goBackBtn)

        lotNumber = intent.getStringExtra("lotNumber") ?: ""
        totalSpaces = intent.getIntExtra("totalSpaces", 1)
        pricePerHour = intent.getDoubleExtra("pricePerHour", 0.0)
        ownerId = intent.getStringExtra("ownerId") ?: ""


        lotNumberText.text = "Booking Lot: $lotNumber"

        setupDurationSpinner()
//        setupSpaceSpinner()

//        dateText.setOnClickListener {
//            val now = Calendar.getInstance()
//            val datePicker = DatePickerDialog(this, { _, year, month, day ->
//                val cal = Calendar.getInstance()
//                cal.set(year, month, day)
//                bookingDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
//                dateText.text = "Date: $bookingDate"
//            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
//            datePicker.show()
//        }

        dateText.setOnClickListener {
            val now = Calendar.getInstance()
            val datePicker = DatePickerDialog(this, { _, year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day)
                bookingDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
                dateText.text = "Date: $bookingDate"

                // 👉 Check time slots after date is selected
                checkAvailableTimeSlots()
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }


//        startTimeText.setOnClickListener {
//            val now = Calendar.getInstance()
//            val timePicker = TimePickerDialog(this, { _, hour, minute ->
//                val cal = Calendar.getInstance()
//                cal.set(Calendar.HOUR_OF_DAY, hour)
//                cal.set(Calendar.MINUTE, minute)
//                startTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
//                startTimeText.text = "Start Time: $startTime"
//
//                // 👇 Automatically check available spaces right after time is picked
//                if (bookingDate != null && durationSpinner.selectedItem != null) {
//                    checkAvailableSpacesAndSetupSpinner()
//                }
//
//            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false)
//            timePicker.show()
//        }


        // check Firestore for any pending unpaid booking for this user + lot
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("pending_bookings")
            .whereEqualTo("clientId", userId)
            .whereEqualTo("lotNumber", lotNumber)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val doc = result.documents[0]
                    bookingDocId = doc.id
                    val createdAt = doc.getLong("timestamp") ?: 0L
                    val elapsed = System.currentTimeMillis() - createdAt
                    val remaining = 5 * 60 * 1000 - elapsed

                    if (remaining > 0) {
                        start5MinTimer(remaining)
                    } else {
                        // Time already expired, force delete
                        doc.reference.delete()
                        Toast.makeText(this, "Booking expired. Try again.", Toast.LENGTH_LONG).show()
                    }
                }
            }


        makePaymentBtn.setOnClickListener {
            if (bookingDate == null || startTime == null) {
                Toast.makeText(this, "Select date and time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = auth.currentUser?.uid ?: return@setOnClickListener

            val durationHours = durationSpinner.selectedItem.toString().toInt()
            val totalPrice = pricePerHour * durationHours * selectedSpaces

            // Calculate end time
            val startCal = Calendar.getInstance()
            val startTimeParts = startTime?.split(":") ?: listOf()
            if (startTimeParts.size == 2) {
                val hourPart = startTimeParts[0].toIntOrNull() ?: 0
                val minutePart = startTimeParts[1].filter { it.isDigit() }.toIntOrNull() ?: 0
                val isPM = startTime?.contains("PM") == true

                startCal.set(Calendar.HOUR, hourPart)
                startCal.set(Calendar.MINUTE, minutePart)
                startCal.set(Calendar.AM_PM, if (isPM) Calendar.PM else Calendar.AM)
                startCal.add(Calendar.HOUR_OF_DAY, durationHours)
            }
            val endTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(startCal.time)
            val endDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(startCal.time)

            val bookingDateFormatted = android.text.format.DateFormat.format("dd/MM/yyyy hh:mm a", System.currentTimeMillis()).toString()

            val booking = hashMapOf(
                "clientId" to userId,
                "ownerId" to ownerId,
                "lotNumber" to lotNumber,
                "date" to bookingDate,
                "startTime" to startTime,
                "endTime" to endTime,
                "endDate" to endDate,
                "durationHours" to durationHours,
                "spacesBooked" to selectedSpaces,
                "pricePerHour" to pricePerHour,
                "totalPrice" to totalPrice,
                "status" to "pending",
                "timestamp" to System.currentTimeMillis(),
                "booking_date" to bookingDateFormatted

            )

            firestore.collection("pending_bookings")
                .add(booking)
                .addOnSuccessListener { documentRef ->
                    val paymentIntent = Intent(this, PaymentActivity::class.java).apply {
                        putExtra("bookingDocId", documentRef.id)
                        putExtra("lotNumber", lotNumber)
                        putExtra("spacesBooked", selectedSpaces)
                        putExtra("pricePerHour", pricePerHour)
                        putExtra("bookingDate", bookingDate)

                    }
                    startActivity(paymentIntent)
                }

                .addOnFailureListener {
                    Toast.makeText(this, "Failed to store booking", Toast.LENGTH_SHORT).show()
                }

        }


        goBackBtn.setOnClickListener {
            finish()
        }
    }

    private fun setupDurationSpinner() {
//        val durations = (1..12).map { "$it" } // 1 to 12 hours
        val durations = (1..12).map { "$it" } // this makes index 0 = 1

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, durations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        durationSpinner.adapter = adapter

        durationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (bookingDate != null && startTime != null) {
                    checkAvailableSpacesAndSetupSpinner()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }


    private fun setupSpaceSpinner(availableSpaces: Int) {
        if (availableSpaces <= 0) {
            Toast.makeText(this, "No spaces available for this time", Toast.LENGTH_LONG).show()
            spaceSpinner.adapter = null
            selectedSpaces = 0
            makePaymentBtn.isEnabled = false // ❌ Disable payment button
            return
        }

        val spaces = (1..availableSpaces).map { "$it" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spaces)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spaceSpinner.adapter = adapter
        makePaymentBtn.isEnabled = true // ✅ Enable button if spaces available

        spaceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedSpaces = spaces[position].toInt()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }




    private fun start5MinTimer(remainingMillis: Long = 5 * 60 * 1000) {
        timer = object : CountDownTimer(remainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Optional: show UI countdown
            }

            override fun onFinish() {
                if (bookingDocId != null) {
                    val docRef = firestore.collection("pending_bookings").document(bookingDocId!!)
                    docRef.get().addOnSuccessListener { snapshot ->
                        if (snapshot.exists() && snapshot.getString("status") == "pending") {
                            docRef.delete()
                            // Roll back spaces
                            firestore.collection("parking_lots")
                                .whereEqualTo("lotNumber", lotNumber)
                                .get()
                                .addOnSuccessListener { result ->
                                    if (!result.isEmpty) {
                                        val lotDoc = result.documents[0]
                                        val currentSpaces = lotDoc.getLong("totalSpaces") ?: 0L
                                        val updatedSpaces = currentSpaces + selectedSpaces
                                        firestore.collection("parking_lots")
                                            .document(lotDoc.id)
                                            .update("totalSpaces", updatedSpaces)
                                    }
                                }

                            Toast.makeText(this@BookingActivity, "Booking expired. Rolling back.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }.start()
    }

    private fun checkAvailableSpacesAndSetupSpinner() {
        val selectedDate = bookingDate ?: return
        val selectedStartTime = startTime ?: return
        val durationHours = durationSpinner.selectedItem?.toString()?.toIntOrNull() ?: return

        // Parse selected start and end times
        val inputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val selectedStart = inputFormat.parse(selectedStartTime) ?: return
        val startCal = Calendar.getInstance().apply { time = selectedStart }
        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.HOUR_OF_DAY, durationHours)
        }

        firestore.collection("pending_bookings")
            .whereEqualTo("lotNumber", lotNumber)
            .whereEqualTo("date", selectedDate)
            .get()
            .addOnSuccessListener { result ->
                var occupiedSpaces = 0

                for (doc in result.documents) {
                    val otherStartStr = doc.getString("startTime") ?: continue
                    val otherDuration = doc.getLong("durationHours")?.toInt() ?: continue
                    val otherSpaces = doc.getLong("spacesBooked")?.toInt() ?: 0

                    val otherStart = inputFormat.parse(otherStartStr) ?: continue
                    val otherStartCal = Calendar.getInstance().apply { time = otherStart }
                    val otherEndCal = (otherStartCal.clone() as Calendar).apply {
                        add(Calendar.HOUR_OF_DAY, otherDuration)
                    }

                    // ⏰ Check for time overlap
                    val overlaps = startCal.before(otherEndCal) && endCal.after(otherStartCal)
                    if (overlaps) {
                        occupiedSpaces += otherSpaces
                    }
                }

                val availableSpaces = (totalSpaces - occupiedSpaces).coerceAtLeast(0)
                setupSpaceSpinner(availableSpaces)
            }
    }

    private fun checkAvailableTimeSlots() {
        val selectedDate = bookingDate ?: return
        val inputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timeSlots = listOf(
            "01:00 AM", "02:00 AM", "03:00 AM", "04:00 AM",
            "05:00 AM", "06:00 AM", "07:00 AM",
            "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM",
            "12:00 PM", "01:00 PM", "02:00 PM", "03:00 PM",
            "04:00 PM", "05:00 PM", "06:00 PM", "07:00 PM",
            "08:00 PM", "09:00 PM", "10:00 PM", "11:00 PM",
            "12:00 AM"
        )

        firestore.collection("pending_bookings")
            .whereEqualTo("lotNumber", lotNumber)
            .whereEqualTo("date", selectedDate)
            .get()
            .addOnSuccessListener { result ->
                val slotAvailability = mutableMapOf<String, Int>()

                for (slot in timeSlots) {
                    val slotStart = Calendar.getInstance().apply {
                        time = inputFormat.parse(slot)!!
                    }
                    val slotEnd = (slotStart.clone() as Calendar).apply {
                        add(Calendar.HOUR_OF_DAY, 1)
                    }

                    var occupied = 0

                    for (doc in result.documents) {
                        val otherStartStr = doc.getString("startTime") ?: continue
                        val otherDuration = doc.getLong("durationHours")?.toInt() ?: continue
                        val otherSpaces = doc.getLong("spacesBooked")?.toInt() ?: 0

                        val parsedStart = inputFormat.parse(otherStartStr) ?: continue
                        val otherStart = Calendar.getInstance().apply {
                            time = parsedStart
                        }

                        val otherEnd = (otherStart.clone() as Calendar).apply {
                            add(Calendar.HOUR_OF_DAY, otherDuration)
                        }

                        if (slotStart.before(otherEnd) && slotEnd.after(otherStart)) {
                            occupied += otherSpaces
                        }
                    }

                    val available = (totalSpaces - occupied).coerceAtLeast(0)
                    slotAvailability[slot] = available
                }

                showTimeSlotDialog(slotAvailability)
            }
    }

    private fun showTimeSlotDialog(slotMap: Map<String, Int>) {
        val availableSlots = slotMap.entries.filter { it.value > 0 }
            .map { "${it.key} (${it.value} spaces left)" }
            .toTypedArray()

        if (availableSlots.isEmpty()) {
            Toast.makeText(this, "No slots available on selected date", Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Select Time Slot")
            .setItems(availableSlots) { _, which ->
                val selected = availableSlots[which]
                val timeOnly = selected.substringBefore(" (")
                startTime = timeOnly
                startTimeText.text = "Start Time: $startTime"

                // Force duration = 1 hour
                durationSpinner.setSelection(0)

                // Setup space spinner based on availability
                val availableSpaces = slotMap[timeOnly] ?: 1
                setupSpaceSpinner(availableSpaces)
            }
            .show()
    }

}
