package com.example.mytrackerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class TimesheetAdapter(private val listener: OnTimesheetItemClickListener) : RecyclerView.Adapter<TimesheetAdapter.ViewHolder>() {

    private val timesheetList: MutableList<TimesheetItem> = mutableListOf()

    fun setTimesheets(timesheets: List<TimesheetItem>) {
        timesheetList.clear()
        timesheetList.addAll(timesheets)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.timesheet_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val timesheet = timesheetList[position]
        holder.bind(timesheet)
    }

    override fun getItemCount(): Int = timesheetList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Bind timesheet data to the UI elements
        fun bind(item: TimesheetItem) {
            // Set labels
            itemView.findViewById<TextView>(R.id.textViewDateLabel).text = "Date:"
            itemView.findViewById<TextView>(R.id.textViewTimeLabel).text = "Time:"
            itemView.findViewById<TextView>(R.id.textViewDescriptionLabel).text = "Description:"
            itemView.findViewById<TextView>(R.id.textViewCategoryLabel).text = "Category:"

            // Set data
            itemView.findViewById<TextView>(R.id.textViewDate).text = item.date
            itemView.findViewById<TextView>(R.id.textViewTime).text = "${item.startTime} - ${item.endTime}"
            itemView.findViewById<TextView>(R.id.textViewDescription).text = item.description
            itemView.findViewById<TextView>(R.id.textViewCategory).text = item.category

            // Load image using Glide
            Glide.with(itemView.context)
                .load(item.photoUri)
                .placeholder(R.drawable.ic_placeholder) // Placeholder image while loading
                .error(R.drawable.ic_error) // Error image if loading fails
                .into(itemView.findViewById(R.id.imageViewPhoto))

            // Set click listener for delete button
            itemView.findViewById<Button>(R.id.buttonDelete).setOnClickListener {
                listener.onDeleteItemClicked(item)
            }
        }
    }

}
