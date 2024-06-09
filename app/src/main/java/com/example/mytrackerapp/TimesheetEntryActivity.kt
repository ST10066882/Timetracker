package com.example.mytrackerapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import android.Manifest
import android.content.pm.PackageManager
import android.widget.ImageButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class TimesheetEntryActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var categories: List<String>
    private var selectedPhotoUri: Uri? = null
    private lateinit var timePickerTag: String
    private val STORAGE_PERMISSION_REQUEST_CODE = 1001


    private lateinit var imageViewPhoto: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timesheet_entry)

        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val buttonSelectDate = findViewById<Button>(R.id.buttonSelectDate)
        val buttonSelectStartTime = findViewById<Button>(R.id.buttonSelectStartTime)
        val buttonSelectEndTime = findViewById<Button>(R.id.buttonSelectEndTime)
        val editTextDescription = findViewById<EditText>(R.id.editTextDescription)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val buttonAddPhoto = findViewById<Button>(R.id.buttonAddPhoto)
        imageViewPhoto = findViewById(R.id.imageViewPhoto)

        val buttonSaveTimesheet = findViewById<Button>(R.id.buttonSaveTimesheet)
        val buttonViewTimesheets = findViewById<Button>(R.id.buttonViewTimesheets)

        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed() // This method navigates back to the previous activity
        }

        buttonViewTimesheets.setOnClickListener {
            // Create an intent to navigate to the view timesheets activity
            val intent = Intent(this, ViewTimesheetActivity::class.java)
            // Start the activity
            startActivity(intent)
        }

        loadCategories(spinnerCategory)

        val calendar = Calendar.getInstance()
        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            buttonSelectDate.text = "$year-${month + 1}-$dayOfMonth"
        }
        val timeListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            when (timePickerTag) {
                "startTime" -> buttonSelectStartTime.text = String.format("%02d:%02d", hourOfDay, minute)
                "endTime" -> buttonSelectEndTime.text = String.format("%02d:%02d", hourOfDay, minute)
            }
        }

        buttonSelectDate.setOnClickListener {
            DatePickerDialog(this, dateListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        buttonSelectStartTime.setOnClickListener {
            timePickerTag = "startTime"
            TimePickerDialog(this, timeListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        buttonSelectEndTime.setOnClickListener {
            timePickerTag = "endTime"
            TimePickerDialog(this, timeListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }


        buttonAddPhoto.setOnClickListener {
            requestStoragePermission()
            // Create intent to either open camera or choose from gallery
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            // Choose from gallery option
            val chooser = Intent.createChooser(galleryIntent, "Choose from...")
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

            // Start the activity for result
            startActivityForResult(chooser, 1)
        }

        buttonSaveTimesheet.setOnClickListener {
            val date = buttonSelectDate.text.toString()
            val startTime = buttonSelectStartTime.text.toString()
            val endTime = buttonSelectEndTime.text.toString()
            val description = editTextDescription.text.toString()
            val category = spinnerCategory.selectedItem.toString()

            if (date == "Select Date" || startTime == "Select Start Time" || endTime == "Select End Time" || description.isEmpty() || category.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                saveTimesheetEntry(date, startTime, endTime, description, category)
            }
        }
    }

    private fun loadCategories(spinner: Spinner) {
        db.collection("categories").get().addOnSuccessListener { documents ->
            categories = documents.map { it.getString("name") ?: "" }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
    }

    private fun saveTimesheetEntry(date: String, startTime: String, endTime: String, description: String, category: String) {
        // Split the date string into year, month, and day components
        val components = date.split("-")

        // Ensure leading zeros for single-digit month and day components
        val formattedDate = "${components[0]}-${components[1].padStart(2, '0')}-${components[2].padStart(2, '0')}"

        val timesheet = hashMapOf(
            "date" to formattedDate,
            "startTime" to startTime,
            "endTime" to endTime,
            "description" to description,
            "category" to category
        )

        // Check if a photo is selected
        if (selectedPhotoUri != null) {
            // If a photo is selected, include its URI in the timesheet entry
            timesheet["photoUri"] = selectedPhotoUri.toString()
        }

        // Save the formatted date to Firestore
        db.collection("timesheets").add(timesheet)
            .addOnSuccessListener {
                Toast.makeText(this, "Timesheet entry saved", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving timesheet entry: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            // Check if the result is from the camera or gallery
            if (data.data != null) {
                // Result is from gallery
                selectedPhotoUri = data.data!!
            } else {
                // Result is from camera
                val imageBitmap = data.extras?.get("data") as Bitmap
                // Convert bitmap to URI and store
                selectedPhotoUri = createImageFile(imageBitmap)
            }
            imageViewPhoto.apply {
                setImageURI(selectedPhotoUri)
                visibility = ImageView.VISIBLE
            }
        }
    }


    // Helper method to create a temporary file and return its URI
    private fun createImageFile(bitmap: Bitmap): Uri {
        // Create a temporary file to store the image
        val tempFile = File.createTempFile(
            "temp_image",  /* prefix */
            ".jpg",  /* suffix */
            applicationContext.cacheDir /* directory */
        )

        // Write the bitmap data to the file
        val outputStream = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.close()

        // Return the URI of the temporary file
        return FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            tempFile
        )
    }

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


}
