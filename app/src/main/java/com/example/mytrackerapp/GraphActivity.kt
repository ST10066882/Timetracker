package com.example.mytrackerapp

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.components.LegendEntry
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class GraphActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var toolbar: Toolbar
    private var showTotalHours = true
    private var showMinGoal = true
    private var showMaxGoal = true
    private lateinit var dailyDataList: List<DailyData>
    private lateinit var progressBarLoading: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        progressBarLoading = findViewById(R.id.progressBarLoading)

        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed() // This method navigates back to the previous activity
        }

        lineChart = findViewById(R.id.lineChart)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (savedInstanceState != null) {
            showTotalHours = savedInstanceState.getBoolean("SHOW_TOTAL_HOURS", true)
            showMinGoal = savedInstanceState.getBoolean("SHOW_MIN_GOAL", true)
            showMaxGoal = savedInstanceState.getBoolean("SHOW_MAX_GOAL", true)
        }
        fetchDataForGraph()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("SHOW_TOTAL_HOURS", showTotalHours)
        outState.putBoolean("SHOW_MIN_GOAL", showMinGoal)
        outState.putBoolean("SHOW_MAX_GOAL", showMaxGoal)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_toggle_total_hours)?.isChecked = showTotalHours
        menu?.findItem(R.id.action_toggle_min_goal)?.isChecked = showMinGoal
        menu?.findItem(R.id.action_toggle_max_goal)?.isChecked = showMaxGoal
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_graph, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_toggle_total_hours -> {
                showTotalHours = !showTotalHours
                item.isChecked = showTotalHours
            }
            R.id.action_toggle_min_goal -> {
                showMinGoal = !showMinGoal
                item.isChecked = showMinGoal
            }
            R.id.action_toggle_max_goal -> {
                showMaxGoal = !showMaxGoal
                item.isChecked = showMaxGoal
            }
            else -> return super.onOptionsItemSelected(item)
        }
        updateGraph()
        return true
    }

    private fun fetchDataForGraph() {

        progressBarLoading.visibility = View.VISIBLE
        firestore.collection("timesheets")
            .get()
            .addOnSuccessListener { timesheetResult ->
                val totalHoursMap = mutableMapOf<String, Double>()

                for (document in timesheetResult) {
                    val date = document.getString("date") ?: continue
                    val startTime = document.getString("startTime") ?: continue
                    val endTime = document.getString("endTime") ?: continue

                    val totalHours = calculateTotalHours(startTime, endTime)
                    totalHoursMap[date] = totalHoursMap.getOrDefault(date, 0.0) + totalHours
                }

                fetchDailyGoalsForGraph(totalHoursMap)
            }
            .addOnFailureListener { exception ->
                // Handle failure
                progressBarLoading.visibility = View.GONE
            }
    }

    private fun fetchDailyGoalsForGraph(totalHoursMap: Map<String, Double>) {
        firestore.collection("dailyGoals")
            .document("user_goals")
            .get()
            .addOnSuccessListener { document ->
                val minDailyHours = document.getDouble("minDailyHours") ?: 0.0
                val maxDailyHours = document.getDouble("maxDailyHours") ?: 0.0

                dailyDataList = totalHoursMap.map { (date, totalHours) ->
                    DailyData(date, totalHours, minDailyHours, maxDailyHours)
                }.sortedBy { it.date }

                displayDataOnGraph(dailyDataList)
            }
            .addOnFailureListener { exception ->
                // Handle failure
                progressBarLoading.visibility = View.GONE
            }
    }


    private fun updateGraph() {
        if (::dailyDataList.isInitialized) { // Check if dailyDataList is initialized
            Log.d("GraphActivity", "Updating graph with filters - showTotalHours: $showTotalHours, showMinGoal: $showMinGoal, showMaxGoal: $showMaxGoal")
            fetchDataForGraph()
        } else {
            // Handle case where dailyDataList is not initialized
            Log.e("GraphActivity", "dailyDataList has not been initialized")
            // You may want to fetch data again or handle this case differently based on your app's logic
        }
    }


    private fun displayDataOnGraph(dailyDataList: List<DailyData>) {
        val noDataTextView: TextView = findViewById(R.id.noDataTextView)
        val entries = ArrayList<Entry>()
        val minGoalEntries = ArrayList<Entry>()
        val maxGoalEntries = ArrayList<Entry>()

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val chartFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

        dailyDataList.forEachIndexed { index, dailyData ->
            entries.add(Entry(index.toFloat(), dailyData.totalHours.toFloat()))
            minGoalEntries.add(Entry(index.toFloat(), dailyData.minGoal.toFloat()))
            maxGoalEntries.add(Entry(index.toFloat(), dailyData.maxGoal.toFloat()))
        }

        if (entries.isEmpty()) {
            noDataTextView.visibility = View.VISIBLE
            lineChart.visibility = View.GONE
            progressBarLoading.visibility = View.GONE
            return
        } else {
            noDataTextView.visibility = View.GONE
            lineChart.visibility = View.VISIBLE
        }

        val dataSets = mutableListOf<ILineDataSet>()

        try {
            Log.d("GraphActivity", "showTotalHours: $showTotalHours, showMinGoal: $showMinGoal, showMaxGoal: $showMaxGoal")

            Log.d("GraphActivity", "Total entries size: ${entries.size}")
            Log.d("GraphActivity", "Min Goal entries size: ${minGoalEntries.size}")
            Log.d("GraphActivity", "Max Goal entries size: ${maxGoalEntries.size}")

            if (showTotalHours && entries.isNotEmpty()) {
                val dataSet = LineDataSet(entries, "Total Hours Worked").apply {
                    color = resources.getColor(android.R.color.holo_blue_dark, null)
                }
                dataSets.add(dataSet)
            }

            if (showMinGoal && minGoalEntries.isNotEmpty()) {
                val minGoalDataSet = LineDataSet(minGoalEntries, "Min Goal").apply {
                    color = resources.getColor(android.R.color.holo_green_dark, null)
                }
                dataSets.add(minGoalDataSet)
            }

            if (showMaxGoal && maxGoalEntries.isNotEmpty()) {
                val maxGoalDataSet = LineDataSet(maxGoalEntries, "Max Goal").apply {
                    color = resources.getColor(android.R.color.holo_red_dark, null)
                }
                dataSets.add(maxGoalDataSet)
            }

            Log.d("GraphActivity", "DataSets size: ${dataSets.size}")
            for ((index, dataSet) in dataSets.withIndex()) {
                Log.d("GraphActivity", "DataSet $index - label: ${dataSet.label}, entries count: ${dataSet.entryCount}")
            }

            if (dataSets.isEmpty()) {
                lineChart.clear()
                lineChart.invalidate()
                return
            }

            val lineData = LineData(dataSets)
            lineChart.data = lineData

            lineChart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    val dateIndex = value.toInt()
                    if (dateIndex in dailyDataList.indices) {
                        val date = dateFormatter.parse(dailyDataList[dateIndex].date)
                        return chartFormatter.format(date)
                    }
                    return ""
                }
            }
            lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM

            // Ensure the legend is correctly updated
            lineChart.legend.isEnabled = true
            lineChart.legend.form = Legend.LegendForm.LINE

            lineChart.data?.dataSets?.forEachIndexed { index, dataSet ->
                dataSet.label = if (dataSet.label.isNullOrBlank()) "DataSet $index" else dataSet.label
            }



            lineChart.axisLeft.axisMinimum = 0f
            lineChart.axisRight.axisMinimum = 0f

            lineChart.invalidate()

            progressBarLoading.visibility = View.GONE
        } catch (e: Exception) {
            Log.e("GraphActivity", "Error displaying data on graph", e)
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
}
