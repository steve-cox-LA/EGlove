package com.example.gloveworks30

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



object FirebaseUploader {
    private var database: DatabaseReference? = null
    private var initialized = false

    fun initializeIfNeeded(firebaseUrl: String, token: String) {
        if (!initialized) {
            database = FirebaseDatabase.getInstance(firebaseUrl).reference
            initialized = true
        }
    }

    fun uploadSensorData(
        firstName: String?,
        lastName: String?,
        sensorData: List<Pair<Float, Float>>
    ) {
        if (!initialized || database == null) return

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val dataMap = mapOf(
            "firstName" to (firstName ?: "N/A"),
            "lastName" to (lastName ?: "N/A"),
            "timestamp" to timestamp,
            "sensorData" to sensorData.map { mapOf("time" to it.first, "magnitude" to it.second) }
        )

        val uniqueKey = database!!.push().key ?: timestamp
        database!!.child("readings").child(uniqueKey).setValue(dataMap)
    }
}
