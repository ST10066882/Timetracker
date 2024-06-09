package com.example.mytrackerapp

data class TimesheetItem(

    val id: String = "",
    val date: String,
    val startTime: String,
    val endTime: String,
    val description: String,
    val category: String,
    val photoUri:String?
)
