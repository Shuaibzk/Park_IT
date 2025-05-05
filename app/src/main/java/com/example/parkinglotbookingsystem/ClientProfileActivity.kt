package com.example.parkinglotbookingsystem

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClientProfileActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_profile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        userId = auth.currentUser?.uid ?: return

        val nameText = findViewById<TextView>(R.id.profileName)
        val emailText = findViewById<TextView>(R.id.profileEmail)
        val totalBookedText = findViewById<TextView>(R.id.totalBooked)
        val changePasswordBtn = findViewById<Button>(R.id.changePasswordBtn)
        val deleteAccountBtn = findViewById<Button>(R.id.deleteAccountBtn)
        val logoutBtn = findViewById<Button>(R.id.logoutBtn)

        // Load name & email
        firestore.collection("users").document(userId).get().addOnSuccessListener {
            nameText.text = "Name: ${it.getString("name")}"
        }
        emailText.text = "Email: ${auth.currentUser?.email}"

        // Load booking count
        firestore.collection("pending_bookings")
            .whereEqualTo("clientId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                var count = 0
                for (doc in snapshot.documents) {
                    count += 1
                }
                totalBookedText.text = "Total Bookings: $count"
            }


        // Logout
        logoutBtn.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Change password (sends email)
        changePasswordBtn.setOnClickListener {
            showChangePasswordDialog()
        }


        // Delete account
        deleteAccountBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Account?")
                .setMessage("This will permanently delete your account and bookings.")
                .setPositiveButton("Delete") { _, _ -> deleteAccount() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun deleteAccount() {
        val user = auth.currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        // Delete user document
        db.collection("users").document(user.uid).delete()

        // Delete their bookings
        db.collection("confirmed_bookings").whereEqualTo("clientId", user.uid).get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
            }

        db.collection("pending_bookings").whereEqualTo("clientId", user.uid).get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
            }

        // Delete from FirebaseAuth
        user.delete().addOnSuccessListener {
            Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showChangePasswordDialog() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val currentPass = EditText(this).apply {
            hint = "Current Password"
        }
        val newPass = EditText(this).apply {
            hint = "New Password"
        }
        layout.addView(currentPass)
        layout.addView(newPass)

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(layout)
            .setPositiveButton("Change") { _, _ ->
                val current = currentPass.text.toString()
                val newPassword = newPass.text.toString()
                reAuthenticateAndChangePassword(current, newPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reAuthenticateAndChangePassword(currentPassword: String, newPassword: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email ?: return

        val credential = EmailAuthProvider.getCredential(email, currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Re-authentication failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
