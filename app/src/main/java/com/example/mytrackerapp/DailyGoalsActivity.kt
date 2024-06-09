package com.example.mytrackerapp

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore

class DailyGoalsActivity : AppCompatActivity() {

    private lateinit var minDailyHoursPicker: NumberPicker
    private lateinit var maxDailyHoursPicker: NumberPicker
    private lateinit var saveGoalsButton: Button
    private val firestore = FirebaseFirestore.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_goals)

        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed() // This method navigates back to the previous activity
        }


        minDailyHoursPicker = findViewById(R.id.min_daily_hours_picker)
        maxDailyHoursPicker = findViewById(R.id.max_daily_hours_picker)
        saveGoalsButton = findViewById(R.id.save_goals_button)

        // Set default values for the NumberPickers
        minDailyHoursPicker.minValue = 1
        minDailyHoursPicker.maxValue = 24
        minDailyHoursPicker.value = 1

        maxDailyHoursPicker.minValue = 1
        maxDailyHoursPicker.maxValue = 24

        saveGoalsButton.setOnClickListener {
            saveGoals()
        }

    }


    private fun saveGoals() {
        val minDailyHours = minDailyHoursPicker.value
        val maxDailyHours = maxDailyHoursPicker.value

        if (minDailyHours > maxDailyHours) {
            Toast.makeText(this, "Min hours cannot be greater than max hours", Toast.LENGTH_SHORT).show()
            return
        }

        val goals = hashMapOf(
            "minDailyHours" to minDailyHours,
            "maxDailyHours" to maxDailyHours
        )

        firestore.collection("dailyGoals").document("user_goals")
            .set(goals)
            .addOnSuccessListener {
                Toast.makeText(this, "Goals saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save goals: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
   }
