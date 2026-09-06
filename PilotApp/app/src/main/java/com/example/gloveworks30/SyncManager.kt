package com.example.gloveworks30

import android.content.Context
import com.example.gloveworks30.localdb.AppDatabase
import com.example.gloveworks30.localdb.MedicationHistoryEntity
import com.example.gloveworks30.localdb.SymptomSubmissionEntity
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

object SyncManager {
    suspend fun syncMedicationHistoryFromFirebase(context: Context): Int {
        if (!isResearchOptedIn(context)) return 0
        val userId = getUserId(context) ?: return 0
        val ref = FirebaseDatabase.getInstance().reference
            .child("users").child(userId)
            .child("symptoms").child("medication")
        val snapshot = ref.get().await()
        val db = AppDatabase.get(context)
        var count = 0
        snapshot.children.forEach { entrySnap ->
            val key = entrySnap.key ?: return@forEach
            val timestamp = entrySnap.child("timestamp").getValue(Long::class.java) ?: 0L
            val medsSnap = entrySnap.child("answers").child("medications")
            val arr = JSONArray()
            medsSnap.children.forEach { medItem ->
                val name = medItem.child("name").getValue(String::class.java) ?: ""
                val dosage = medItem.child("dosage").getValue(String::class.java) ?: ""
                val frequency = medItem.child("frequency").getValue(String::class.java) ?: ""
                if (name.isNotBlank()) {
                    arr.put(
                        JSONObject().apply {
                            put("name", name)
                            put("dosage", dosage)
                            put("frequency", frequency)
                        }
                    )
                }
            }
            db.appDao().upsertMedicationHistory(
                MedicationHistoryEntity(
                    firebaseKey = key,
                    timestamp = timestamp,
                    medsJson = arr.toString()
                )
            )
            count++
        }
        return count
    }
    suspend fun syncCategoryFromFirebase(context: Context, category: String): Int {
        if (!isResearchOptedIn(context)) return 0
        val userId = getUserId(context) ?: return 0
        val ref = FirebaseDatabase.getInstance().reference
            .child("users").child(userId)
            .child("symptoms").child(category)
        val snapshot = ref.get().await()
        val db = AppDatabase.get(context)
        var count = 0
        snapshot.children.forEach { entrySnap ->
            val key = entrySnap.key ?: return@forEach
            val timestamp = entrySnap.child("timestamp").getValue(Long::class.java) ?: 0L
            val answersNode = entrySnap.child("answers")
            val obj = JSONObject()
            answersNode.children.forEach { ans ->
                val k = ans.key ?: return@forEach
                obj.put(k, ans.getValue(String::class.java) ?: ans.value?.toString())
            }
            db.appDao().upsertSymptomSubmission(
                SymptomSubmissionEntity(
                    firebaseKey = key,
                    category = category,
                    timestamp = timestamp,
                    answersJson = obj.toString()
                )
            )
            count++
        }
        return count
    }
    suspend fun syncAllFromFirebase(context: Context): Int {
        var total = 0
        total += syncMedicationHistoryFromFirebase(context)
        total += syncCategoryFromFirebase(context, "lifestyle")
        total += syncCategoryFromFirebase(context, "physical")
        total += syncCategoryFromFirebase(context, "psychological")
        return total
    }
}