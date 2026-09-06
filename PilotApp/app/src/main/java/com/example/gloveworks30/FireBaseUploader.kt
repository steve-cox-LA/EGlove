package com.example.gloveworks30

import android.content.Context
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

private fun getParticipantId(context: Context): String? {
    val prefs = context.getSharedPreferences("gloveworks_prefs", Context.MODE_PRIVATE)
    return prefs.getString("participant_id", null)
}

fun uploadVoiceEpochToFirebase(
    context: Context,
    epochIndex: Int,
    rms: Float,
    dominantFreq: Float,
    fftData: List<Pair<Float, Float>>,
    waveformEnvelope: List<Float>
) {
    val participantId = getParticipantId(context) ?: run {
        Log.w("Upload", "No participant ID set, skipping voice upload")
        return
    }
    val sharedPref = context.getSharedPreferences("gloveworks_prefs", Context.MODE_PRIVATE)
    val sessionId = sharedPref.getString("voice_session_id", null)
        ?: UUID.randomUUID().toString().also {
            sharedPref.edit().putString("voice_session_id", it).apply()
        }

    val fftAsMaps = fftData.map { (freq, mag) ->
        mapOf("freq" to freq, "magnitude" to mag)
    }

    val dataMap = mapOf(
        "timestamp" to System.currentTimeMillis(),
        "epoch" to epochIndex,
        "rms" to rms,
        "dominantFreq" to dominantFreq,
        "fftProfile" to fftAsMaps,
        "waveformEnvelope" to waveformEnvelope
    )

    CoroutineScope(Dispatchers.IO).launch {
        FirebaseDatabase.getInstance().reference
            .child("participants")
            .child(participantId)
            .child("voice")
            .child(sessionId)
            .child("epoch_$epochIndex")
            .setValue(dataMap)
            .addOnSuccessListener {
                Log.d("Upload", "Voice epoch $epochIndex uploaded for $participantId")
            }
            .addOnFailureListener { e ->
                Log.e("Upload", "Failed to upload voice: ${e.message}", e)
            }
    }
}

fun uploadMotionEpochToFirebase(
    context: Context,
    epochIndex: Int,
    accelerometerSamples: List<MotionSample>
) {
    val participantId = getParticipantId(context) ?: run {
        Log.w("Upload", "No participant ID set, skipping motion upload")
        return
    }
    val sharedPref = context.getSharedPreferences("gloveworks_prefs", Context.MODE_PRIVATE)
    val sessionId = sharedPref.getString("motion_session_id", null)
        ?: UUID.randomUUID().toString().also {
            sharedPref.edit().putString("motion_session_id", it).apply()
        }

    val samplesAsMaps = accelerometerSamples.map { s ->
        mapOf(
            "t" to s.t,
            "ax" to s.ax, "ay" to s.ay, "az" to s.az,
            "gx" to s.gx, "gy" to s.gy, "gz" to s.gz
        )
    }

    CoroutineScope(Dispatchers.IO).launch {
        val db = FirebaseDatabase.getInstance().reference
        val epochRef = db
            .child("participants")
            .child(participantId)
            .child("motion")
            .child(sessionId)
            .child("epoch_$epochIndex")

        val dataMap = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "epoch" to epochIndex,
            "samples" to samplesAsMaps
        )
        epochRef.setValue(dataMap)
            .addOnSuccessListener {
                Log.d("Upload", "Motion epoch $epochIndex uploaded for $participantId")
            }
            .addOnFailureListener { e ->
                Log.e("Upload", "Failed to upload motion: ${e.message}", e)
            }
    }
}

fun uploadUserInfoToFirebase(context: Context, info: Map<String, Any?>) {
    val participantId = getParticipantId(context) ?: return
    CoroutineScope(Dispatchers.IO).launch {
        FirebaseDatabase.getInstance().reference
            .child("participants")
            .child(participantId)
            .child("profile")
            .setValue(info)
    }
}

fun saveSymptomAnswersToFirebase(
    context: Context,
    category: String,
    answers: Map<String, Any?>,
    onComplete: (Boolean) -> Unit = {}
) {
    val participantId = getParticipantId(context) ?: run {
        onComplete(false)
        return
    }
    val entryId = System.currentTimeMillis().toString()
    val payload = mapOf(
        "timestamp" to System.currentTimeMillis(),
        "category" to category.lowercase(),
        "answers" to answers
    )
    FirebaseDatabase.getInstance().reference
        .child("participants")
        .child(participantId)
        .child("symptoms")
        .child(category.lowercase())
        .child(entryId)
        .setValue(payload)
        .addOnSuccessListener { onComplete(true) }
        .addOnFailureListener { onComplete(false) }
}