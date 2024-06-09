package com.example.mytrackerapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class CategoryAdapter(private var categories: MutableList<String>, private val context: Context) :
    RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryTextView: TextView = itemView.findViewById(R.id.textViewCategoryName)
        val categoryIconView: ImageView = itemView.findViewById(R.id.imageViewCategoryIcon)
        val buttonDeleteCategory: Button = itemView.findViewById(R.id.buttonDeleteCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.categoryTextView.text = category
        holder.categoryIconView.setImageResource(R.drawable.ic_folder)
        holder.buttonDeleteCategory.setOnClickListener {
            showDeleteConfirmationDialog(position)
        }
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    fun updateCategories(newCategories: List<String>) {
        categories.clear()
        categories.addAll(newCategories)
        notifyDataSetChanged()
    }

    private fun showDeleteConfirmationDialog(position: Int) {
        val categoryNameToDelete = categories[position]

        val alertDialog = AlertDialog.Builder(context)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete the category \"$categoryNameToDelete\"?")
            .setPositiveButton("Yes") { _, _ ->
                onDeleteCategoryClick(position)
            }
            .setNegativeButton("No", null)
            .create()

        alertDialog.show()
    }

    private fun onDeleteCategoryClick(position: Int) {
        val categoryNameToDelete = categories[position]

        val db = FirebaseFirestore.getInstance()
        val categoriesRef = db.collection("categories")

        // Query Firestore to find the document ID based on the category name
        categoriesRef.whereEqualTo("name", categoryNameToDelete)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Retrieve the document ID
                    val documentId = querySnapshot.documents[0].id

                    // Delete the document using the retrieved document ID
                    categoriesRef.document(documentId)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error deleting category: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Category not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error querying categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

}
