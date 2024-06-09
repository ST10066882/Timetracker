package com.example.mytrackerapp


import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ViewTotalHoursActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TotalHoursAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var buttonDateRangePicker: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var noRecordsTextView: TextView


    private var selectedStartDate: Calendar? = null
    private var selectedEndDate: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_total_hours)

        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed() // This method navigates back to the previous activity
        }

        db = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.recyclerViewTotalHours)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TotalHoursAdapter()
        recyclerView.adapter = adapter

        progressBar = findViewById(R.id.progressBarLoading)
        noRecordsTextView = findViewById(R.id.textViewNoRecords)

        buttonDateRangePicker = findViewById(R.id.buttonDateRangePicker)
        buttonDateRangePicker.setOnClickListener {
            showDateRangePicker()
        }

        fetchTotalHoursPerCategory()
    }

    private fun showDateRangePicker() {
        val datePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Date Range")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = Calendar.getInstance().apply {
                timeInMillis = selection.first ?: 0
            }
            val endDate = Calendar.getInstance().apply {
                timeInMillis = selection.second ?: 0
            }

            selectedStartDate = startDate
            selectedEndDate = endDate

            fetchTotalHoursForSelectedDateRange() // Call the correct function here
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }


    private fun fetchTotalHoursPerCategory() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        noRecordsTextView.visibility = View.GONE

        db.collection("timesheets")
            .get()
            .addOnSuccessListener { result ->
                val categoryHoursMap = mutableMapOf<String, Double>() // Change Long to Double

                for (document in result) {
                    val category = document.getString("category") ?: continue
                    val startTime = document.getString("startTime") ?: continue
                    val endTime = document.getString("endTime") ?: continue

                    val totalHours = calculateTotalHours(startTime, endTime) // Use calculateTotalHours function
                    categoryHoursMap[category] = categoryHoursMap.getOrDefault(category, 0.0) + totalHours // Change 0L to 0.0
                }

                val categoryHoursList = categoryHoursMap.map { (category, totalHours) ->
                    CategoryHours(category, totalHours)
                }

                adapter.setCategoryHours(categoryHoursList)
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
            .addOnFailureListener { exception ->
                Log.w("ViewTotalHoursActivity", "Error getting documents: ", exception)
                Toast.makeText(this, "Error fetching data", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
    }

    private fun fetchTotalHoursPerCategoryInRange() {
        if (selectedStartDate == null || selectedEndDate == null) {
            Toast.makeText(this, "Please select a date range", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        noRecordsTextView.visibility = View.GONE

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedStartDate = dateFormat.format(selectedStartDate!!.time)
        val formattedEndDate = dateFormat.format(selectedEndDate!!.time)

        db.collection("timesheets")
            .whereGreaterThanOrEqualTo("date", formattedStartDate)
            .whereLessThanOrEqualTo("date", formattedEndDate)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    noRecordsTextView.visibility = View.VISIBLE
                    adapter.setCategoryHours(emptyList())
                } else {
                    noRecordsTextView.visibility = View.GONE

                    val categoryHoursMap = mutableMapOf<String, Double>()

                    for (document in result) {
                        val category = document.getString("category") ?: continue
                        val startTime = document.getString("startTime") ?: continue
                        val endTime = document.getString("endTime") ?: continue

                        val totalHours = calculateTotalHours(startTime, endTime)
                        categoryHoursMap[category] = categoryHoursMap.getOrDefault(category, 0.0) + totalHours
                    }

                    val categoryHoursList = categoryHoursMap.map { (category, totalHours) ->
                        CategoryHours(category, totalHours)
                    }

                    adapter.setCategoryHours(categoryHoursList)
                }

                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
            .addOnFailureListener { exception ->
                Log.w("ViewTotalHoursActivity", "Error getting documents: ", exception)
                Toast.makeText(this, "Error fetching data", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
    }


    // Call this function when the user selects a date range
    private fun fetchTotalHoursForSelectedDateRange() {
        if (selectedStartDate != null && selectedEndDate != null) {
            fetchTotalHoursPerCategoryInRange()
        } else {
            fetchTotalHoursPerCategory()
        }
    }


    private fun calculateTotalHours(startTime: String, endTime: String): Double {
        val startHour = startTime.split(":")[0].toDouble()
        val endHour = endTime.split(":")[0].toDouble()
        val duration = if (endHour < startHour) {
            (endHour + 24) - startHour
        } else {
            endHour - startHour
        }
        return duration
    }

// Remove millisToHours function as it's not needed anymore


    private fun timeToMillis(time: String): Long {
        val parts = time.split(":")
        val hours = parts[0].toInt()
        val minutes = parts[1].toInt()
        return (hours * 3600 + minutes * 60) * 1000L
    }

    private fun millisToHours(millis: Long): Double {
        return millis / 3600000.0
    }
}
