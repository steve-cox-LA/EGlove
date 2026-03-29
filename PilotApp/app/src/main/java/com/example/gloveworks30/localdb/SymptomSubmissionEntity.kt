package com.example.gloveworks30.localdb

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "symptom_submissions")
data class SymptomSubmissionEntity(
    @PrimaryKey val firebaseKey: String,
    val category: String,
    val timestamp: Long,
    val answersJson: String
)