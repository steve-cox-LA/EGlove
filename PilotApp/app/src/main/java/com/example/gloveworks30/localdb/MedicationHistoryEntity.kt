package com.example.gloveworks30.localdb

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_history")
data class MedicationHistoryEntity(
    @PrimaryKey val firebaseKey: String,
    val timestamp: Long,
    val medsJson: String
)