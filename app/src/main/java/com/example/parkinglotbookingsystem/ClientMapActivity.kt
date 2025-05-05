package com.example.parkinglotbookingsystem

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore

class ClientMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var firestore: FirebaseFirestore
    private var userLocation: LatLng? = null

    private var highlightLat: Double? = null
    private var highlightLon: Double? = null
    private var highlightLotNumber: String? = null


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_map)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        firestore = FirebaseFirestore.getInstance()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        highlightLat = intent.getDoubleExtra("highlightLat", Double.NaN)
        highlightLon = intent.getDoubleExtra("highlightLon", Double.NaN)
        highlightLotNumber = intent.getStringExtra("highlightLotNumber")

    }



    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        requestUserLocation()

        mMap.setOnMarkerClickListener { marker ->
            showParkingLotBottomSheet(marker)
            true
        }
    }

    private fun requestUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                userLocation = LatLng(location.latitude, location.longitude)
                mMap.isMyLocationEnabled = true
                loadParkingLots()
            } else {
                Toast.makeText(this, "Failed to get current location", Toast.LENGTH_SHORT).show()
            }
        }
    }

//    private fun loadParkingLots() {
//        firestore.collection("parking_lots")
//            .get()
//            .addOnSuccessListener { documents ->
//                for (document in documents) {
//                    val latitude = document.getDouble("latitude") ?: continue
//                    val longitude = document.getDouble("longitude") ?: continue
//                    val ownerId = document.getString("ownerId") ?: continue
//                    val parkingLotNumber = document.getString("parkingLotNumber") ?: "Unknown"
//                    val price = document.getDouble("pricePerHour")?.toString() ?: "Unknown"
//                    val totalSpaces = document.getLong("totalSpaces")?.toString() ?: "Unknown"
//                    val description = document.getString("description") ?: "No description"
//
//                    val location = LatLng(latitude, longitude)
//
//                    firestore.collection("users").document(ownerId).get()
//                        .addOnSuccessListener { ownerDoc ->
//                            val ownerName = ownerDoc.getString("name") ?: "Owner Unknown"
//
//                            val marker = mMap.addMarker(
//                                MarkerOptions()
//                                    .position(location)
//                                    .title(ownerName)
//                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
//                            )
//                            marker?.tag = "$parkingLotNumber###$price###$totalSpaces###$description###$ownerId"
//                        }
//                }
//
//                userLocation?.let {
//                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 14f))
//                }
//            }
//    }

    private fun loadParkingLots() {
        val collection = firestore.collection("parking_lots")

        if (highlightLat != null && !highlightLat!!.isNaN()
            && highlightLon != null && !highlightLon!!.isNaN()
            && !highlightLotNumber.isNullOrEmpty()
        ) {
            // Load only the highlighted lot
            collection
                .whereEqualTo("latitude", highlightLat)
                .whereEqualTo("longitude", highlightLon)
                .whereEqualTo("parkingLotNumber", highlightLotNumber)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Toast.makeText(this, "Parking lot not found", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val document = documents.first()
                    addMarkerFromDocument(document)
                    val target = LatLng(highlightLat!!, highlightLon!!)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 16f))
                }
        } else {
            // Load all lots normally (for "Show on Map")
            collection.get().addOnSuccessListener { documents ->
                for (document in documents) {
                    addMarkerFromDocument(document)
                }

                userLocation?.let {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 14f))
                }
            }
        }
    }

    private fun addMarkerFromDocument(document: com.google.firebase.firestore.DocumentSnapshot) {
        val latitude = document.getDouble("latitude") ?: return
        val longitude = document.getDouble("longitude") ?: return
        val ownerId = document.getString("ownerId") ?: return
        val parkingLotNumber = document.getString("parkingLotNumber") ?: "Unknown"
        val price = document.getDouble("pricePerHour")?.toString() ?: "Unknown"
        val totalSpaces = document.getLong("totalSpaces")?.toString() ?: "Unknown"
        val description = document.getString("description") ?: "No description"

        val location = LatLng(latitude, longitude)

        firestore.collection("users").document(ownerId).get()
            .addOnSuccessListener { ownerDoc ->
                val ownerName = ownerDoc.getString("name") ?: "Owner Unknown"

                val marker = mMap.addMarker(
                    MarkerOptions()
                        .position(location)
                        .title(ownerName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )
                marker?.tag = "$parkingLotNumber###$price###$totalSpaces###$description###$ownerId"
            }
    }


    private fun showParkingLotBottomSheet(marker: Marker) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_parking_info, null)

        val lotNumberText = view.findViewById<TextView>(R.id.lotNumberText)
        val priceText = view.findViewById<TextView>(R.id.priceText)
        val totalSpacesText = view.findViewById<TextView>(R.id.totalSpacesText)
        val descriptionText = view.findViewById<TextView>(R.id.descriptionText)
        val bookNowBtn = view.findViewById<Button>(R.id.bookNowBtn)
        val closeBtn = view.findViewById<Button>(R.id.closeBtn)
        val ratingText = view.findViewById<TextView>(R.id.averageRatingText)
        val reviewText = view.findViewById<TextView>(R.id.recentReviewText)


        val tagData = (marker.tag as? String)?.split("###") ?: listOf()

        val parkingLotNumber = tagData.getOrNull(0) ?: "Unknown"
        val price = tagData.getOrNull(1) ?: "Unknown"
        val totalSpaces = tagData.getOrNull(2) ?: "Unknown"
        val description = tagData.getOrNull(3) ?: "No description available"
        val ownerId = tagData.getOrNull(4) ?: ""

        lotNumberText.text = "Lot: $parkingLotNumber"
        priceText.text = "Price: $price BDT/hour"
        totalSpacesText.text = "Total Spaces: $totalSpaces"
        descriptionText.text = "Details: $description"

        // setting rating and review
        firestore.collection("reviews")
            .whereEqualTo("lotNumber", parkingLotNumber)
            .get()
            .addOnSuccessListener { snapshot ->
                val ratings = snapshot.documents.mapNotNull { it.getDouble("rating") }
                val reviews = snapshot.documents.mapNotNull { it.getString("review") }

                if (ratings.isNotEmpty()) {
                    val avg = ratings.average()
                    ratingText.text = "Rating: %.1f".format(avg)
                } else {
                    ratingText.text = "Rating: N/A"
                }

                if (reviews.isNotEmpty()) {
                    reviewText.text = "Latest review: ${reviews.last()}"
                } else {
                    reviewText.text = "Latest review: Not available"
                }
            }
            .addOnFailureListener {
                ratingText.text = "Rating: N/A"
                reviewText.text = "Latest review: Not available"
            }



        closeBtn.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bookNowBtn.setOnClickListener {
            val intent = Intent(this, BookingActivity::class.java).apply {
                putExtra("lotNumber", parkingLotNumber)
                putExtra("pricePerHour", price.toDoubleOrNull() ?: 0.0)
                putExtra("totalSpaces", totalSpaces.toIntOrNull() ?: 1)
                putExtra("ownerId", ownerId)
            }
            startActivity(intent)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestUserLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
