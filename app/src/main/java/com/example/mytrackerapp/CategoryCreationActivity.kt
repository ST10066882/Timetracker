package com.example.mytrackerapp

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class CategoryCreationActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var categoryListener: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_creation)

        db = FirebaseFirestore.getInstance()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewCategories)
        recyclerView.layoutManager = LinearLayoutManager(this)
        categoryAdapter = CategoryAdapter(mutableListOf(), this)
        recyclerView.adapter = categoryAdapter

        findViewById<FloatingActionButton>(R.id.fabAddCategory).setOnClickListener {
            showAddCategoryDialog()
        }

        fetchCategories()

        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed() // This method navigates back to the previous activity
        }
    }

    private fun fetchCategories() {
        categoryListener = db.collection("categories")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error fetching categories", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val categories = snapshots?.documents?.map { it.getString("name") ?: "" } ?: emptyList()
                categoryAdapter.updateCategories(categories)
            }
    }

    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null)
        val editTextCategoryName = dialogView.findViewById<EditText>(R.id.editTextCategoryName)
        val buttonAddCategory = dialogView.findViewById<Button>(R.id.buttonAddCategory)

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Add Category")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        buttonAddCategory.setOnClickListener {
            val categoryName = editTextCategoryName.text.toString()
            if (TextUtils.isEmpty(categoryName)) {
                Toast.makeText(this, "Enter category name", Toast.LENGTH_SHORT).show()
            } else {
                addCategoryToFirestore(categoryName)
                alertDialog.dismiss()
            }
        }

        alertDialog.show()
    }

    private fun addCategoryToFirestore(categoryName: String) {
        val category = hashMapOf("name" to categoryName)
        db.collection("categories").add(category)
            .addOnSuccessListener {
                Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error adding category", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        categoryListener.remove()

    }
}
