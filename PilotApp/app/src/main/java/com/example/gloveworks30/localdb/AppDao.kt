package com.example.gloveworks30.localdb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Symptoms (Lifestyle/Physical/Psychological)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSymptomSubmission(entity: SymptomSubmissionEntity)
    @Query("SELECT * FROM symptom_submissions ORDER BY timestamp DESC")
    fun observeSymptomSubmissions(): Flow<List<SymptomSubmissionEntity>>
    @Query("SELECT * FROM symptom_submissions WHERE category = :category ORDER BY timestamp DESC")
    fun observeSymptomSubmissionsByCategory(category: String): Flow<List<SymptomSubmissionEntity>>
    // Medication History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedicationHistory(entity: MedicationHistoryEntity)
    @Query("SELECT * FROM medication_history ORDER BY timestamp DESC")
    fun observeMedicationHistory(): Flow<List<MedicationHistoryEntity>>
    @Query("DELETE FROM medication_history")
    suspend fun clearMedicationHistory()
}