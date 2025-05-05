package com.example.parkinglotbookingsystem

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val nameInput = findViewById<EditText>(R.id.nameInput)
        val userTypeGroup = findViewById<RadioGroup>(R.id.userTypeGroup)
        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val name = nameInput.text.toString().trim()

            val selectedRoleId = userTypeGroup.checkedRadioButtonId
            val role = when (selectedRoleId) {
                R.id.clientOption -> "client"
                R.id.ownerOption -> "owner"
                else -> ""
            }

            if (email.isEmpty() || password.isEmpty() || name.isEmpty() || role.isEmpty()) {
                Toast.makeText(this, "Please fill all fields and select a role", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Register the user in Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid

                        // Save additional info to Firestore
                        val userData = hashMapOf(
                            "email" to email,
                            "name" to name,
                            "role" to role,
                            "banned" to false
                        )

                        firestore.collection("users").document(uid!!)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}