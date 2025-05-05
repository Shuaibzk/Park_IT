package com.example.parkinglotbookingsystem

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

data class User(
    val id: String = "",
    var name: String = "",
    val email: String = "",
    val role: String = "",
    var banned: Boolean = false
)

class ViewUsersActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private val firestore = FirebaseFirestore.getInstance()
    private val users = mutableListOf<User>()
    private lateinit var userAdapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_users)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(users, ::showEditDialog, ::confirmDelete)
        recyclerView.adapter = userAdapter

        fetchUsers()
    }

    private fun fetchUsers() {
        firestore.collection("users").get().addOnSuccessListener { snapshot ->
            users.clear()
            for (doc in snapshot) {
                val user = User(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    role = doc.getString("role") ?: "",
                    banned = doc.getBoolean("banned") ?: false
                )
                users.add(user)
            }
            userAdapter.notifyDataSetChanged()
        }
    }

    private fun showEditDialog(user: User) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_user, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.editName)
        val statusSpinner = dialogView.findViewById<Spinner>(R.id.spinnerStatus)

        nameInput.setText(user.name)
        statusSpinner.setSelection(if (user.banned) 1 else 0)


        AlertDialog.Builder(this)
            .setTitle("Edit User")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val updatedName = nameInput.text.toString()
                val updatedBanned = statusSpinner.selectedItem.toString() == "Banned"
                firestore.collection("users").document(user.id)
                    .update(mapOf("name" to updatedName, "banned" to updatedBanned))
                    .addOnSuccessListener {
                        user.name = updatedName
                        user.banned = updatedBanned
                        userAdapter.notifyItemChanged(users.indexOfFirst { it.id == user.id })
                    }


            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.name}?")
            .setPositiveButton("Delete") { _, _ ->
                firestore.collection("users").document(user.id).delete()
                    .addOnSuccessListener { fetchUsers() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

class UserAdapter(
    private val userList: List<User>,
    private val onEdit: (User) -> Unit,
    private val onDelete: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvEmail: TextView = view.findViewById(R.id.tvEmail)
        val tvRole: TextView = view.findViewById(R.id.tvRole)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val btnEdit: Button = view.findViewById(R.id.btnEdit)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.tvName.text = "Name: ${user.name}"
        holder.tvEmail.text = "Email: ${user.email}"
        holder.tvRole.text = "Role: ${user.role}"
        holder.tvStatus.text = "Status: ${if (user.banned) "Banned" else "Active"}"
        holder.btnEdit.setOnClickListener { onEdit(user) }
        holder.btnDelete.setOnClickListener { onDelete(user) }
    }

    override fun getItemCount(): Int = userList.size
}
