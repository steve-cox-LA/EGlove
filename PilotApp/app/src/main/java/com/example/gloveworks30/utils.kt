package com.example.gloveworks30

import android.content.Context
import com.example.gloveworks30.localdb.AppDatabase
import com.example.gloveworks30.localdb.MedicationHistoryEntity
import com.example.gloveworks30.localdb.SymptomSubmissionEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

const val PREFS_NAME = "gloveworks_prefs"
const val KEY_RESEARCH_OPT_IN = "research_opt_in"
const val KEY_USER_ID = "user_id"

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

fun observeMedicationHistoryLocal(context: Context): Flow<List<MedicationHistoryEntity>> {
    val db = AppDatabase.get(context)
    return db.appDao().observeMedicationHistory()
}

fun isResearchOptedIn(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_RESEARCH_OPT_IN, false)
}

fun getUserId(context: Context): String? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_USER_ID, null)
}

fun setResearchOptIn(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_RESEARCH_OPT_IN, enabled).apply()
}

fun setUserId(context: Context, userId: String?) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_USER_ID, userId).apply()
}

fun generateUserId(
    isAnonymous: Boolean,
    firstName: String = "",
    lastName: String = "",
    dob: String = ""
): String {
    return if (isAnonymous) {
        val letters = (1..4).map { ('A'..'Z').random() }.joinToString("")
        val numbers = (100..999).random().toString()
        "glw_anon_${letters}${numbers}"
    } else {
        val fPart = firstName.take(3).padEnd(3, '-').lowercase()
        val lPart = lastName.take(3).padEnd(3, '-').lowercase()
        val dobDigits = dob.filter { it.isDigit() }.take(8).padEnd(8, '0')
        "glw_${fPart}_${lPart}${dobDigits}"
    }
}
