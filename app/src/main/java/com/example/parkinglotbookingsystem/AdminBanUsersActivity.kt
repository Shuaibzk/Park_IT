package com.example.parkinglotbookingsystem

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class AdminBanUsersActivity : AppCompatActivity() {

    data class UserItem(
        val id: String,
        val name: String,
        val role: String,
        var banned: Boolean
    )

    private val userList = mutableListOf<UserItem>()
    private lateinit var listView: ListView
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_ban_users)

        listView = findViewById(R.id.banUserListView)

        loadUsers()
    }

    private fun loadUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                userList.clear()
                for (doc in snapshot) {
                    val id = doc.id
                    val name = doc.getString("name") ?: "Unnamed"
                    val role = doc.getString("role") ?: "unknown"
                    val banned = doc.getBoolean("banned") ?: false

                    if (role == "client" || role == "owner") {
                        userList.add(UserItem(id, name, role, banned))
                    }
                }
                populateList()
            }
    }

    private fun populateList() {
        val adapter = object : ArrayAdapter<UserItem>(this, android.R.layout.simple_list_item_1, userList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val context = this@AdminBanUsersActivity
                val item = getItem(position)!!

                val layout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 16, 16, 16)
                    setBackgroundColor(Color.parseColor("#F5F5F5"))
                }

                val nameText = TextView(context).apply {
                    text = "Name: ${item.name} (${item.role})"
                    setTextSize(16f)
                }

                val buttonLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                }

                val banBtn = Button(context).apply {
                    text = "Ban"
                    setBackgroundColor(Color.parseColor("#FF3B30")) // red
                    setTextColor(Color.WHITE)

                    if (item.banned) {
                        isEnabled = false
                        alpha = 0.5f
                    } else {
                        isEnabled = true
                        alpha = 1.0f
                        setOnClickListener { banUser(item) }
                    }
                }

                val unbanBtn = Button(context).apply {
                    text = "Unban"
                    setBackgroundColor(Color.parseColor("#4CAF50")) // green
                    setTextColor(Color.WHITE)

                    if (!item.banned) {
                        isEnabled = false
                        alpha = 0.5f
                    } else {
                        isEnabled = true
                        alpha = 1.0f
                        setOnClickListener { unbanUser(item) }
                    }
                }

                buttonLayout.addView(banBtn)
                buttonLayout.addView(unbanBtn)

                layout.addView(nameText)
                layout.addView(buttonLayout)

                val wrapper = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                }
                wrapper.addView(layout)
                wrapper.addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 24)
                })

                return wrapper
            }
        }

        listView.adapter = adapter
    }



    private fun banUser(user: UserItem) {
        db.collection("users").document(user.id)
            .update("banned", true)
            .addOnSuccessListener {
                user.banned = true
                Toast.makeText(this, "User banned", Toast.LENGTH_SHORT).show()
                if (user.role == "client") {
                    banClient(user.id)
                } else {
                    banOwner(user.id)
                }
                populateList()
            }
    }

    private fun banClient(userId: String) {
        db.collection("pending_bookings").whereEqualTo("clientId", userId).get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
            }

        db.collection("confirmed_bookings").whereEqualTo("clientId", userId).get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
            }
    }

    private fun unbanUser(user: UserItem) {
        db.collection("users").document(user.id)
            .update("banned", false)
            .addOnSuccessListener {
                user.banned = false
                Toast.makeText(this, "User unbanned", Toast.LENGTH_SHORT).show()
                populateList()
            }
    }


    private fun banOwner(userId: String) {
        db.collection("parking_lots").whereEqualTo("ownerId", userId).get()
            .addOnSuccessListener { lots ->
                for (lot in lots.documents) {
                    val lotNumber = lot.getString("lotNumber") ?: continue

                    db.collection("pending_bookings").whereEqualTo("lotNumber", lotNumber).get()
                        .addOnSuccessListener { bookings ->
                            for (booking in bookings.documents) {
                                booking.reference.delete()
                            }
                        }

                    db.collection("confirmed_bookings").whereEqualTo("lotNumber", lotNumber).get()
                        .addOnSuccessListener { bookings ->
                            for (booking in bookings.documents) {
                                booking.reference.delete()
                            }
                        }

                    lot.reference.delete()
                }
            }
    }
}
