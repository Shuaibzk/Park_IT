package com.example.parkinglotbookingsystem
// show on maps or show on lists
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ClientSearchResultActivity : AppCompatActivity() {

    private lateinit var showListBtn: Button
    private lateinit var showMapBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_search_result)

        showListBtn = findViewById(R.id.showListBtn)
        showMapBtn = findViewById(R.id.showMapBtn)

        val district = intent.getStringExtra("district")
        val date = intent.getStringExtra("date")
        val startTime = intent.getStringExtra("startTime")
        val endTime = intent.getStringExtra("endTime")

        showListBtn.setOnClickListener {
            val intent = Intent(this, ClientFilterActivity::class.java)
            startActivity(intent)
        }

        showMapBtn.setOnClickListener {
            val intent = Intent(this, ClientMapActivity::class.java)
//            intent.putExtra("district", district)
            intent.putExtra("date", date)
            intent.putExtra("startTime", startTime)
            intent.putExtra("endTime", endTime)
            startActivity(intent)
        }
    }
}
