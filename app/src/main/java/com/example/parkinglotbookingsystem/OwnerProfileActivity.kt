package com.example.parkinglotbookingsystem

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OwnerProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView
    private lateinit var totalLots: TextView
    private lateinit var totalSpaces: TextView
    private lateinit var changePasswordBtn: Button
    private lateinit var deleteAccountBtn: Button
    private lateinit var logoutBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owner_profile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        profileName = findViewById(R.id.profileName)
        profileEmail = findViewById(R.id.profileEmail)
        totalLots = findViewById(R.id.totalLots)
        totalSpaces = findViewById(R.id.totalSpaces)
        changePasswordBtn = findViewById(R.id.changePasswordBtn)
        deleteAccountBtn = findViewById(R.id.deleteAccountBtn)
        logoutBtn = findViewById(R.id.logoutBtn)

        loadProfileData()

        changePasswordBtn.setOnClickListener {
            showChangePasswordDialog()
        }


        deleteAccountBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account? This cannot be undone!")
                .setPositiveButton("Yes") { _, _ ->
                    deleteOwnerAccount()
                }
                .setNegativeButton("No", null)
                .show()
        }

        logoutBtn.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadProfileData() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Fetch Name and Email
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name")
                    val email = document.getString("email")

                    profileName.text = "Name: ${name ?: "Unknown"}"
                    profileEmail.text = "Email: ${email ?: "Unknown"}"
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }

        // Now fetch Approved Parking Lots count
        firestore.collection("parking_lots")
            .whereEqualTo("ownerId", uid)
            .get()
            .addOnSuccessListener { approvedResult ->

                val approvedCount = approvedResult.size()
                var spaceCount = 0

                for (doc in approvedResult) {
                    val spaces = doc.getLong("totalSpaces") ?: 0
                    spaceCount += spaces.toInt()
                }

                // Now fetch Pending Parking Lots count
                firestore.collection("pending_parking_lots")
                    .whereEqualTo("ownerId", uid)
                    .get()
                    .addOnSuccessListener { pendingResult ->

                        val pendingCount = pendingResult.size()

                        // Now update UI
                        totalLots.text = "Approved: $approvedCount | Pending: $pendingCount"
                        totalSpaces.text = "Total Spaces: $spaceCount"

                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to load pending lots", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load approved lots", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteOwnerAccount() {
        val uid = auth.currentUser?.uid

        if (uid != null) {
            // Delete user document
            firestore.collection("users").document(uid).delete()
            firestore.collection("parking_lots")
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener { result ->
                    for (doc in result) {
                        doc.reference.delete()
                    }
                }

            // Delete auth user
            auth.currentUser?.delete()
                ?.addOnSuccessListener {
                    Toast.makeText(this, "Account deleted successfully.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                ?.addOnFailureListener {
                    Toast.makeText(this, "Error deleting account. Please re-login and try again.", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun showChangePasswordDialog() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(32, 16, 32, 16)

        val currentInput = EditText(this).apply {
            hint = "Current Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val newInput = EditText(this).apply {
            hint = "New Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(currentInput)
        layout.addView(newInput)

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(layout)
            .setPositiveButton("Change") { _, _ ->
                val currentPassword = currentInput.text.toString().trim()
                val newPassword = newInput.text.toString().trim()

                if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                    Toast.makeText(this, "Please enter both fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                reAuthenticateAndChangePassword(currentPassword, newPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reAuthenticateAndChangePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return

        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to update password: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Re-authentication failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

}