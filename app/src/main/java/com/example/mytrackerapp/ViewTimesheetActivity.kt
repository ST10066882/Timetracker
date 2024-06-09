package com.example.mytrackerapp


import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class ViewTimesheetActivity : AppCompatActivity(), OnTimesheetItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TimesheetAdapter
    private lateinit var db: FirebaseFirestore
    private val STORAGE_PERMISSION_REQUEST_CODE = 1001
    private lateinit var startDateTextView: TextView
    private lateinit var endDateTextView: TextView
    private lateinit var selectDateRangeButton: Button
    private lateinit var applyFilterButton: Button
    private lateinit var resetFilterButton: Button

    private var selectedStartDate: Calendar? = null
    private var selectedEndDate: Calendar? = null
    private lateinit var buttonViewTotalHours: Button
    private lateinit var buttonAddTimesheetEntry: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_timesheet)

        requestStoragePermission()

        db = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.recyclerViewTimesheets)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TimesheetAdapter(this)
        recyclerView.adapter = adapter
        // Initialize views
        startDateTextView = findViewById(R.id.textViewStartDate)
        endDateTextView = findViewById(R.id.textViewEndDate)
        selectDateRangeButton = findViewById(R.id.buttonSelectDateRange)
        applyFilterButton = findViewById(R.id.buttonApplyFilter)
        resetFilterButton = findViewById(R.id.buttonResetFilter)
        buttonAddTimesheetEntry = findViewById(R.id.buttonAddTimesheetEntry)

        buttonViewTotalHours = findViewById(R.id.buttonViewTotalHours)
        buttonViewTotalHours.setOnClickListener {
            // Navigate to ViewTotalHoursActivity
            val intent = Intent(this, ViewTotalHoursActivity::class.java)
            startActivity(intent)
        }

        buttonAddTimesheetEntry.setOnClickListener {
            // Navigate to the Timesheet Entry page
            val intent = Intent(this, TimesheetEntryActivity::class.java)
            startActivity(intent)
        }

        // Set click listener for select date range button
        selectDateRangeButton.setOnClickListener {
            showDatePickerDialog()
        }

        // Set click listener for apply filter button
        applyFilterButton.setOnClickListener {
            applyDateRangeFilter()
        }

        resetFilterButton.setOnClickListener {
            resetFilter()
        }


        fetchTimesheetEntries()
    }

    private fun resetFilter() {
        // Clear the selected start and end dates
        selectedStartDate = null
        selectedEndDate = null

        // Update the UI to reflect the cleared filter
        startDateTextView.text = ""
        endDateTextView.text = ""

        // Optionally, re-fetch all timesheet entries without filtering
        fetchTimesheetEntries()
    }


    private fun showDatePickerDialog() {
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

            updateDateTextViews(startDate, endDate)
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun applyDateRangeFilter() {
        // Ensure both start date and end date are selected
        if (selectedStartDate != null && selectedEndDate != null) {
            val startDate = selectedStartDate!!.time
            val endDate = selectedEndDate!!.time

            // Format start and end dates to yyyy-MM-dd format
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formattedStartDate = dateFormat.format(startDate)
            val formattedEndDate = dateFormat.format(endDate)

            // Query timesheet entries from the database based on the selected date range
            db.collection("timesheets")
                .whereGreaterThanOrEqualTo("date", formattedStartDate)
                .whereLessThanOrEqualTo("date", formattedEndDate)
                .get()
                .addOnSuccessListener { result ->
                    val filteredTimesheets = mutableListOf<TimesheetItem>()
                    for (document in result) {
                        val date = document.getString("date") ?: ""
                        val startTime = document.getString("startTime") ?: ""
                        val endTime = document.getString("endTime") ?: ""
                        val description = document.getString("description") ?: ""
                        val category = document.getString("category") ?: ""
                        val photoUri = document.getString("photoUri")
                        val id = document.id

                        // Create a TimesheetItem object and add it to the filtered list
                        val timesheetItem = TimesheetItem(id, date, startTime, endTime, description, category, photoUri)
                        filteredTimesheets.add(timesheetItem)
                    }

                    // Update the RecyclerView adapter with the filtered data
                    adapter.setTimesheets(filteredTimesheets)

                    if (filteredTimesheets.isEmpty()) {
                        // If no timesheets found within the date range, display a message
                        Toast.makeText(this, "No timesheets found within the selected date range.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "Error getting documents: ", exception)
                }
        } else {
            // Handle case where start date or end date is not selected
            Toast.makeText(this, "Please select both start and end dates.", Toast.LENGTH_SHORT).show()
        }
    }





    private fun updateDateTextViews(startDate: Calendar, endDate: Calendar) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        startDateTextView.text = dateFormat.format(startDate.time)
        endDateTextView.text = dateFormat.format(endDate.time)
    }


    private fun fetchTimesheetEntries() {
        db.collection("timesheets")
            .get()
            .addOnSuccessListener { result ->
                val timesheets = mutableListOf<TimesheetItem>()
                for (document in result) {
                    // Check if the document exists
                    if (document.exists()) {
                        val date = document.getString("date") ?: ""
                        val startTime = document.getString("startTime") ?: ""
                        val endTime = document.getString("endTime") ?: ""
                        val description = document.getString("description") ?: ""
                        val category = document.getString("category") ?: ""
                        val photoUri = document.getString("photoUri")
                        val id = document.id

                        val timesheetItem = TimesheetItem(id, date, startTime, endTime, description, category, photoUri)
                        timesheets.add(timesheetItem)
                    }
                }
                adapter.setTimesheets(timesheets)
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
    }


    companion object {
        const val TAG = "ViewTimesheetActivity"
    }

    // Add this method to request storage permission
    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted
            // You can proceed with accessing media files
        } else {
            // Permission has not been granted yet, request it
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), STORAGE_PERMISSION_REQUEST_CODE)
        }
    }


    // Override onRequestPermissionsResult to handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with accessing media files
            } else {
                // Permission denied, inform the user and handle accordingly
                Toast.makeText(this, "Permission denied. You won't be able to access media files.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDeleteItemClicked(item: TimesheetItem) {
        showDeleteConfirmationDialog(item)
    }

    private fun showDeleteConfirmationDialog(item: TimesheetItem) {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Delete Timesheet Entry")
            .setMessage("Are you sure you want to delete this timesheet entry?")
            .setPositiveButton("Yes") { _, _ ->
                deleteTimesheetEntry(item)
            }
            .setNegativeButton("No", null)
            .create()

        alertDialog.show()
    }

    private fun deleteTimesheetEntry(item: TimesheetItem) {
        db.collection("timesheets").document(item.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Timesheet entry deleted", Toast.LENGTH_SHORT).show()
                fetchTimesheetEntries()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error deleting entry", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "Error deleting document", e)
            }
    }



}
