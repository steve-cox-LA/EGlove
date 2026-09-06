package com.example.gloveworks30

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*
import java.util.*
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.google.firebase.storage.StorageMetadata


fun uploadRawAudioToStorage(
    context: Context,
    epochIndex: Int,
    audioData: List<Short>
) {
    val sharedPref = context.getSharedPreferences("gloveworks_prefs", Context.MODE_PRIVATE)
    if (!sharedPref.getBoolean("share_voice", false)) return

    val userId = sharedPref.getString("user_id", null) ?: return
    val timestamp = System.currentTimeMillis().toString()
    val sessionId = sharedPref.getString("voice_session_id", null)
        ?: UUID.randomUUID().toString().also {
            sharedPref.edit().putString("voice_session_id", it).apply()
        }

    // Convert audio data to ByteArray
    val byteBuffer = ByteBuffer.allocate(audioData.size * 2)
        .order(ByteOrder.LITTLE_ENDIAN)
    audioData.forEach { byteBuffer.putShort(it) }
    val audioBytes = byteBuffer.array()



    // Determine user type
    val userType = sharedPref.getString("type", "anonymous") ?: "anonymous"
    val metadataBuilder = StorageMetadata.Builder()
        .setContentType("audio/pcm")
        .setCustomMetadata("userId", userId)
        .setCustomMetadata("userType", userType)
        .setCustomMetadata("epoch", epochIndex.toString())
        .setCustomMetadata("timestamp", timestamp)

    if (userType == "anonymous") {
        val age = sharedPref.getString("age", "N/A") ?: "N/A"
        val diagnosed = sharedPref.getBoolean("diagnosedWithPD", false)
        val shareMotion = sharedPref.getBoolean("share_motion", false)
        val shareVoice = sharedPref.getBoolean("share_voice", false)

        metadataBuilder
            .setCustomMetadata("age", age)
            .setCustomMetadata("diagnosedWithPD", diagnosed.toString())
            .setCustomMetadata("consent_motion", shareMotion.toString())
            .setCustomMetadata("consent_voice", shareVoice.toString())
    } else if (userType == "gloveworks") {
        val name = sharedPref.getString("name", "N/A") ?: "N/A"
        val lastName = sharedPref.getString("lastName", "N/A") ?: "N/A"
        val email = sharedPref.getString("email", "N/A") ?: "N/A"
        val dob = sharedPref.getString("dob", "N/A") ?: "N/A"
        val phone = sharedPref.getString("phone", "N/A") ?: "N/A"
        val pdqString = sharedPref.getString("pdq", "") ?: ""
        val type = sharedPref.getString("type", "anonymous") ?: "anonymous"



        metadataBuilder
            .setCustomMetadata("name", name)
            .setCustomMetadata("lastName", lastName)
            .setCustomMetadata("email", email)
            .setCustomMetadata("dob", dob)
            .setCustomMetadata("phone", phone)
            .setCustomMetadata("pdq", pdqString)
            .setCustomMetadata("type", type)

    }

    val metadata = metadataBuilder.build()
    val storageRef = FirebaseStorage.getInstance().reference
    val fileRef = storageRef.child("voice_data/$userId/$sessionId/epoch_$epochIndex.pcm")

    fileRef.putBytes(audioBytes, metadata)
        .addOnSuccessListener {
            Log.d("Upload", "Raw audio uploaded to Storage with metadata for epoch $epochIndex")
        }
        .addOnFailureListener { e ->
            Log.e("Upload", "Failed to upload raw audio: ${e.message}", e)
        }
}


fun uploadMotionEpochToFirebase(
    context: Context,
    epochIndex: Int,
    accelerometerSamples: List<Triple<Float, Float, Float>>
) {
    val sharedPref = context.getSharedPreferences("gloveworks_prefs", Context.MODE_PRIVATE)
    if (!sharedPref.getBoolean("share_motion", false)) return

    val userId = sharedPref.getString("user_id", null) ?: return
    val timestamp = System.currentTimeMillis().toString()
    val sessionId = sharedPref.getString("motion_session_id", null)
        ?: UUID.randomUUID().toString().also {
            sharedPref.edit().putString("motion_session_id", it).apply()
        }

    // Convert List<Triple<Float, Float, Float>> to List<Map<String, Float>>
    val samplesAsMaps = accelerometerSamples.map { (x, y, z) ->
        mapOf("x" to x, "y" to y, "z" to z)
    }

    CoroutineScope(Dispatchers.IO).launch {
        val db = FirebaseDatabase.getInstance().reference
        val epochRef = db.child("users").child(userId).child("motion").child(sessionId).child("epoch_$epochIndex")


        val dataMap = mapOf(
            "timestamp" to timestamp,
            "epoch" to epochIndex,
            "rawSamples" to samplesAsMaps
        )

        epochRef.setValue(dataMap)
    }
}

fun uploadUserInfoToFirebase(context: Context, info: Map<String, Any?>) {
    val sharedPref = context.getSharedPreferences("gloveworks_prefs", Context.MODE_PRIVATE)
    val userId = sharedPref.getString("user_id", null) ?: return

    CoroutineScope(Dispatchers.IO).launch {
        val db = FirebaseDatabase.getInstance().reference
        db.child("users").child(userId).setValue(info)
    }
}

