package com.example.parkinglotbookingsystem

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminVerifyComplaintsActivity : AppCompatActivity() {

    data class Complaint(
        val id: String,
        val collection: String,
        val submittedBy: String,
        val role: String,
        val reportType: String,
        val message: String,
        val timestamp: Timestamp?,
        var status: String
    )

    private val db = FirebaseFirestore.getInstance()
    private val allComplaints = mutableListOf<Complaint>()
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_verify_complaints)

        listView = findViewById(R.id.complaintsListView)

        loadComplaints()
    }

    private fun loadComplaints() {
        allComplaints.clear()

        val clientQuery = db.collection("report_client_reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
        val ownerQuery = db.collection("report_owner_reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        clientQuery.get().addOnSuccessListener { clientSnap ->
            val clientList = clientSnap.documents.map {
                Complaint(
                    id = it.id,
                    collection = "report_client_reports",
                    submittedBy = it.getString("clientId") ?: "Unknown Client",
                    role = "client",
                    reportType = it.getString("reportType") ?: "general",
                    message = it.getString("message") ?: "",
                    timestamp = it.getTimestamp("timestamp"),
                    status = it.getString("status") ?: "unverified"
                )
            }

            ownerQuery.get().addOnSuccessListener { ownerSnap ->
                val ownerList = ownerSnap.documents.map {
                    Complaint(
                        id = it.id,
                        collection = "report_owner_reports",
                        submittedBy = it.getString("ownerId") ?: "Unknown Owner",
                        role = "owner",
                        reportType = it.getString("reportType") ?: "general",
                        message = it.getString("message") ?: "",
                        timestamp = it.getTimestamp("timestamp"),
                        status = it.getString("status") ?: "unverified"
                    )
                }

                allComplaints.addAll(clientList + ownerList)
                allComplaints.sortByDescending { it.timestamp }
                populateListView()
            }
        }
    }

    private fun populateListView() {
        val adapter = object : ArrayAdapter<Complaint>(this, android.R.layout.simple_list_item_1, allComplaints) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val context = this@AdminVerifyComplaintsActivity
                val item = getItem(position)!!

                val layout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 16, 16, 16)
                    setBackgroundColor(Color.parseColor("#EEEEEE"))
                }

                val type = TextView(context).apply {
                    text = "Type: ${item.reportType.uppercase()} (${item.role})"
                    setTextColor(Color.BLACK)
                    setTextSize(16f)
                }

                val from = TextView(context).apply {
                    text = "Submitted by: ${item.submittedBy}"
                }

                val msg = TextView(context).apply {
                    text = "Message: ${item.message}"
                }

                val time = TextView(context).apply {
                    text = "Time: ${item.timestamp?.toDate()}"
                }

                val btnLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                }

                val verifyBtn = Button(context).apply {
                    text = "Verify"
                    setBackgroundColor(Color.parseColor("#FFD700"))
                    if (item.status == "verified") {
                        isEnabled = false
                        alpha = 0.5f
                    } else {
                        setOnClickListener {
                            db.collection(item.collection).document(item.id)
                                .update("status", "verified")
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Marked as verified", Toast.LENGTH_SHORT).show()
                                    item.status = "verified"
                                    notifyDataSetChanged()
                                }
                        }
                    }
                }

                val deleteBtn = Button(context).apply {
                    text = "Delete"
                    setBackgroundColor(Color.parseColor("#FF3B30"))
                    setTextColor(Color.WHITE)
                    setOnClickListener {
                        db.collection(item.collection).document(item.id)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                                allComplaints.removeAt(position)
                                notifyDataSetChanged()
                            }
                    }
                }

                btnLayout.addView(verifyBtn)
                btnLayout.addView(deleteBtn)

                layout.addView(type)
                layout.addView(from)
                layout.addView(msg)
                layout.addView(time)
                layout.addView(btnLayout)

                val marginView = View(context)
                marginView.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 24)
                val wrapper = LinearLayout(context)
                wrapper.orientation = LinearLayout.VERTICAL
                wrapper.addView(layout)
                wrapper.addView(marginView)

                return wrapper
            }
        }

        listView.adapter = adapter
    }
}
