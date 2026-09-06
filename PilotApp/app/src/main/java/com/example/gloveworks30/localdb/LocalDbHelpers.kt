package com.example.gloveworks30.localdb

import android.content.Context
import com.example.gloveworks30.MedOption
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

//Medication JSON helpers
fun medsToJson(meds: List<MedOption>): String {
    val arr = JSONArray()
    meds.forEach { med ->
        val obj = JSONObject().apply {
            put("name", med.name)
            put("dosage", med.dosage)
            put("frequency", med.frequency)
        }
        arr.put(obj)
    }
    return arr.toString()
}
fun medsFromJson(raw: String): List<MedOption> {
    return try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("name")
                if (name.isNotBlank()) {
                    add(
                        MedOption(
                            name = name,
                            dosage = obj.optString("dosage"),
                            frequency = obj.optString("frequency")
                        )
                    )
                }
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}
//Local DB write helpers
suspend fun saveMedicationHistoryLocal(context: Context, medsJson: String) {
    val db = AppDatabase.get(context)
    val ts = System.currentTimeMillis()
    db.appDao().upsertMedicationHistory(
        MedicationHistoryEntity(
            firebaseKey = "local_$ts",
            timestamp = ts,
            medsJson = medsJson
        )
    )
}

suspend fun saveSymptomSubmissionLocal(
    context: Context,
    category: String,
    answersJson: String
) {
    val db = AppDatabase.get(context)
    val ts = System.currentTimeMillis()
    db.appDao().upsertSymptomSubmission(
        SymptomSubmissionEntity(
            firebaseKey = "local_$ts",
            category = category,
            timestamp = ts,
            answersJson = answersJson
        )
    )
}

//Local DB read helpers
fun observeMedicationHistoryLocal(context: Context): Flow<List<MedicationHistoryEntity>> {
    val db = AppDatabase.get(context)
    return db.appDao().observeMedicationHistory()
}
fun observeSymptomSubmissionsLocal(context: Context): Flow<List<SymptomSubmissionEntity>> {
    val db = AppDatabase.get(context)
    return db.appDao().observeSymptomSubmissions()
}
fun observeSymptomSubmissionsByCategoryLocal(
    context: Context,
    category: String
): Flow<List<SymptomSubmissionEntity>> {
    val db = AppDatabase.get(context)
    return db.appDao().observeSymptomSubmissionsByCategory(category)
}

suspend fun clearMedicationHistoryLocal(context: Context) {
    val db = AppDatabase.get(context)
    db.appDao().clearMedicationHistory()
}