package com.example.mytrackerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class TotalHoursAdapter : RecyclerView.Adapter<TotalHoursAdapter.ViewHolder>() {

    private val categoryHoursList = mutableListOf<CategoryHours>()

    fun setCategoryHours(categoryHours: List<CategoryHours>) {
        categoryHoursList.clear()
        categoryHoursList.addAll(categoryHours)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_hours, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val categoryHours = categoryHoursList[position]
        holder.bind(categoryHours)
    }

    override fun getItemCount(): Int {
        return categoryHoursList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewCategory = itemView.findViewById<TextView>(R.id.textViewCategory)
        private val textViewTotalHours = itemView.findViewById<TextView>(R.id.textViewTotalHours)

        fun bind(categoryHours: CategoryHours) {
            textViewCategory.text = categoryHours.category
            textViewTotalHours.text = String.format("%.2f hours", categoryHours.totalHours)
        }
    }
}

data class CategoryHours(
    val category: String,
    val totalHours:Double
)
