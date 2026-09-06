package com.example.gloveworks30

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gloveworks30.ui.theme.Gloveworks30Theme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jtransforms.fft.DoubleFFT_1D
import java.nio.charset.Charset
import java.util.UUID
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import com.example.gloveworks30.localdb.clearMedicationHistoryLocal


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            Gloveworks30Theme {
                val navController = rememberNavController()
                val userInfoViewModel: UserInfoViewModel = viewModel()
                val context = LocalContext.current
                NavHost(navController = navController, startDestination = "home") {
                    composable("onboarding") {
                        OnboardingScreen(
                            onComplete = {
                                navController.navigate("home") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        )
                    }
                        composable("home") { HomeScreen(navController) }
                        composable("main") { GaitMonitor(navController, userInfoViewModel) }
                        composable("voice") { VoiceScreen(navController) }
                        composable("settings") { SettingsScreen(navController) }
                        composable("user_info") { UserInfoScreen(navController) }
                        composable("glove_control") { GloveControlScreen(navController) }
                        composable("symptom_tracker") { SymptomTrackerScreen(navController) }
                        composable("symptom_medication") { MedicationsScreen(navController) }
                        composable("Medication_history") { MedicationsHistoryScreen(navController) }
                        composable("symptom_lifestyle") {
                            SymptomQuestionScreen(
                                title = "Lifestyle",
                                questions = lifestyleQuestions(),
                                navController = navController
                            )
                        }
                        composable("symptom_physical") {
                            SymptomQuestionScreen(
                                title = "Physical",
                                questions = physicalQuestions(),
                                navController = navController
                            )
                        }
                        composable("symptom_psychological") {
                            SymptomQuestionScreen(
                                title = "Psychological",
                                questions = psychologicalQuestions(),
                                navController = navController
                            )
                        }
                        composable("about") {
                            AboutScreen(navController)
                        }
                    }
                }
            }
        }
    }

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var isGloveWorksMember by remember { mutableStateOf<Boolean?>(null) }
    // Participant form
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val pdqResponses = remember { mutableStateListOf<Int>().apply { repeat(8) { add(0) } } }
    // Anonymous form
    var shareMotion by remember { mutableStateOf(false) }
    var shareVoice by remember { mutableStateOf(false) }
    var age by remember { mutableStateOf("") }
    var diagnosedPD by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Scaffold(
        bottomBar = {
            if (isGloveWorksMember != null) {
                BottomAppBar {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = {
                        firstName = ""
                        lastName = ""
                        email = ""
                        dob = ""
                        phone = ""
                        pdqResponses.forEachIndexed { index, _ -> pdqResponses[index] = 0 }
                        shareMotion = false
                        shareVoice = false
                        age = ""
                        diagnosedPD = false
                        isGloveWorksMember = null
                    }) {
                        Text("Back")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome to GloveWorks", style = MaterialTheme.typography.headlineSmall)
            if (isGloveWorksMember == null) {
                Text("Are you a participant in the GloveWorks research project?")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { isGloveWorksMember = true }) {
                        Text("Yes")
                    }
                    Button(onClick = { isGloveWorksMember = false }) {
                        Text("No")
                    }
                }
            } else if (isGloveWorksMember == true) {
                val context = LocalContext.current
                var firstName by remember { mutableStateOf("") }
                var lastName by remember { mutableStateOf("") }
                var email by remember { mutableStateOf("") }
                var dob by remember { mutableStateOf("") }
                var phone by remember { mutableStateOf("") }
                val pdqResponses = remember { mutableStateListOf<Int>() }
                val pdqQuestions = listOf(
                    "1. Difficulty carrying out daily activities (e.g., getting dressed)?",
                    "2. Trouble walking short distances?",
                    "3. Difficulty keeping balance or falling over?",
                    "4. Difficulty with handwriting or using utensils?",
                    "5. Emotional well-being affected (e.g., feeling depressed)?",
                    "6. Difficulty speaking clearly?",
                    "7. Experiencing bodily discomfort (pain, cramps, etc.)?",
                    "8. Feeling isolated from others due to your condition?"
                )
                val pdqOptions = listOf("Never", "Occasionally", "Sometimes", "Often", "Always")
                if (pdqResponses.size < pdqQuestions.size) {
                    pdqResponses.addAll(List(pdqQuestions.size) { 0 })
                }
                val isFormValid = firstName.isNotBlank() &&
                        lastName.isNotBlank() &&
                        email.isNotBlank() &&
                        dob.isNotBlank() &&
                        phone.isNotBlank() &&
                        pdqResponses.none { it == 0 }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "GloveWorks Participant Info",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(value = firstName,
                        onValueChange = { firstName = it }, label = { Text("First Name") })
                    OutlinedTextField(value = lastName,
                        onValueChange = { lastName = it }, label = { Text("Last Name") })
                    OutlinedTextField(value = email,
                        onValueChange = { email = it }, label = { Text("Email") })
                    OutlinedTextField(value = dob,
                        onValueChange = { dob = it },
                        label = { Text("Date of Birth, please use the MM/DD/YYYY format.") })
                    OutlinedTextField(value = phone,
                        onValueChange = { phone = it }, label = { Text("Phone") })
                    Text("Parkinson’s Questionnaire", style = MaterialTheme.typography.titleMedium)
                    pdqQuestions.forEachIndexed { index, question ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(question, style = MaterialTheme.typography.bodyMedium)
                            Column {
                                pdqOptions.forEachIndexed { optIndex, label ->
                                    OutlinedButton(
                                        onClick = { pdqResponses[index] = optIndex + 1 },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (pdqResponses[index] == optIndex + 1)
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else Color.Transparent
                                        )
                                    ) {
                                        Text(label)
                                    }
                                }
                            }
                        }
                    }
                    if (!isFormValid) {
                        Text(
                            text = "Please complete all fields and answer all questions.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val userId = generateUserId(
                                isAnonymous = false,
                                firstName = firstName.lowercase(),
                                lastName = lastName.lowercase(),
                                dob = dob
                            )
                            val sharedPref = context.getSharedPreferences(
                                "gloveworks_prefs",
                                Context.MODE_PRIVATE
                            )
                            with(sharedPref.edit()) {
                                putString("user_id", userId)
                                putString("type", "gloveworks")
                                putString("name", firstName)
                                putString("lastName", lastName)
                                putString("email", email)
                                putString("dob", dob)
                                putString("phone", phone)
                                putString("pdq", pdqResponses.joinToString(","))
                                putBoolean("share_motion", true)
                                putBoolean("share_voice", true)
                                apply()
                            }
                            val userMap = mapOf(
                                "type" to "gloveworks",
                                "consent" to true,
                                "profile" to mapOf(
                                    "name" to firstName,
                                    "lastName" to lastName,
                                    "email" to email,
                                    "dob" to dob,
                                    "phone" to phone
                                ),
                                "pdq" to pdqResponses.toList()
                            )
                            uploadUserInfoToFirebase(context, userMap)
                            onComplete()
                        }) {
                        Text("Submit")
                    }
                }
            } else {
                Text("Anonymous Consent", style = MaterialTheme.typography.titleMedium)

                Text(
                    "Please click buttons below to choose the type of anonymous" +
                            " data you would like to share."
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { shareMotion = !shareMotion }) {
                        Text(if (shareMotion) "✓ Motion" else "Motion")
                    }
                    Button(onClick = { shareVoice = !shareVoice }) {
                        Text(if (shareVoice) "✓ Voice" else "Voice")
                    }
                }
                OutlinedTextField(value = age, onValueChange = { age = it },
                    label = { Text("Your Age (optional)") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = diagnosedPD, onCheckedChange = { diagnosedPD = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Diagnosed with Parkinson’s?")
                }
                Button(onClick = {
                    val userId = generateUserId(isAnonymous = true)
                    val sharedPref = context.getSharedPreferences(
                        "gloveworks_prefs",
                        Context.MODE_PRIVATE
                    )
                    with(sharedPref.edit()) {
                        putString("user_id", userId)
                        putString("type", "anonymous")
                        putBoolean("share_motion", shareMotion)
                        putBoolean("share_voice", shareVoice)
                        putString("age", age)
                        putBoolean("diagnosedWithPD", diagnosedPD)
                        apply()
                    }
                    val anonMap = mapOf(
                        "type" to "anonymous",
                        "consent" to mapOf(
                            "motion" to shareMotion,
                            "voice" to shareVoice
                        ),
                        "profile" to mapOf(
                            "age" to age,
                            "diagnosedWithPD" to diagnosedPD
                        )
                    )
                    uploadUserInfoToFirebase(context, anonMap)
                    onComplete()
                }) {
                    Text("Submit")
                }
            }
        }
    }
}

@Composable
fun RealTimeGraph(sensorData: List<Pair<Float, Float>>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary.toArgb()
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(false)
                isDragEnabled = false
                setScaleEnabled(false)
                setPinchZoom(false)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setViewPortOffsets(0f, 0f, 0f, 0f)
                xAxis.apply {
                    isEnabled = false
                    setDrawLabels(false)
                    setDrawGridLines(false)
                    setDrawAxisLine(false)
                }
                axisLeft.apply {
                    isEnabled = true
                    axisMinimum = 0f
                    axisMaximum = 40f
                    setDrawLabels(false)
                    setDrawGridLines(false)
                    setDrawAxisLine(false)
                }
                axisRight.isEnabled = false
                legend.isEnabled = false
            }
        },
        update = { chart ->
            if (sensorData.isNotEmpty()) {
                val maxTime = sensorData.maxOf { it.first }
                val entries = sensorData.map { (time, magnitude) -> Entry(time, magnitude) }

                val dataSet = LineDataSet(entries, "").apply {
                    color = lineColor
                    setDrawCircles(false)
                    setDrawValues(false)
                    lineWidth = 4f
                    mode = LineDataSet.Mode.LINEAR
                }

                chart.data = LineData(dataSet)
                chart.xAxis.axisMinimum = maxTime - 5f
                chart.xAxis.axisMaximum = maxTime

                chart.notifyDataSetChanged()
                chart.invalidate()
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

fun computeFFT(accelerationData: List<Float>): List<Pair<Float, Float>> {
    if (accelerationData.isEmpty()) return emptyList()
    val n = accelerationData.size
    val fft = DoubleFFT_1D(n.toLong())
    val mean = accelerationData.average().toFloat()
    val adjustedData = accelerationData.map { it - mean }
    val fftInput = DoubleArray(n * 2)
    for (i in adjustedData.indices) {
        fftInput[2 * i] = adjustedData[i].toDouble()
        fftInput[2 * i + 1] = 0.0
    }
    fft.realForward(fftInput)
    val fftOutput = mutableListOf<Pair<Float, Float>>()
    val samplingRate = 40f
    val nyquistLimit = samplingRate / 2
    for (i in 0 until n / 2) {
        val real = fftInput[2 * i]
        val imag = fftInput[2 * i + 1]
        val magnitude = sqrt(real.pow(2) + imag.pow(2)).toFloat()
        val power = magnitude.pow(2)
        val frequency = (i.toFloat() / (n / 2)) * nyquistLimit
        if (frequency > 15) break
        fftOutput.add(Pair(frequency, power))
    }
    return fftOutput
}

@Composable
fun FFTGraph(fftData: List<Pair<Float, Float>>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.secondary.toArgb()
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(false)
                isDragEnabled = false
                setScaleEnabled(false)
                setPinchZoom(false)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setViewPortOffsets(0f, 0f, 0f, 0f)
                xAxis.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    setDrawLabels(false)
                    setDrawAxisLine(false)
                }
                axisLeft.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    setDrawLabels(false)
                    setDrawAxisLine(false)
                }
                axisRight.isEnabled = false
                legend.isEnabled = false
            }
        },
        update = { chart ->
            if (fftData.isNotEmpty()) {
                val entries = fftData.map { (freq, power) -> Entry(freq, power) }

                val dataSet = LineDataSet(entries, "").apply {
                    color = lineColor
                    setDrawCircles(false)
                    setDrawValues(false)
                    lineWidth = 4f
                }
                chart.data = LineData(dataSet)
                val maxPower = fftData.maxOfOrNull { it.second } ?: 1000f
                chart.axisLeft.axisMaximum = maxPower * 1.1f
                chart.axisLeft.axisMinimum = 0f

                chart.notifyDataSetChanged()
                chart.invalidate()
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun AudioWaveformGraph(audioData: List<Short>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary.toArgb() // NEW

    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(false)
                setScaleEnabled(false)
                setPinchZoom(false)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setViewPortOffsets(0f, 0f, 0f, 0f)
                xAxis.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    setDrawLabels(false)
                    setDrawAxisLine(false)
                }
                axisLeft.apply {
                    isEnabled = true
                    axisMinimum = -32768f
                    axisMaximum = 32767f
                    setDrawGridLines(false)
                    setDrawLabels(false)
                    setDrawAxisLine(false)
                }
                axisRight.isEnabled = false
                legend.isEnabled = false
            }
        },
        update = { chart ->
            if (audioData.isNotEmpty()) {
                val downsampled = audioData
                    .withIndex()
                    .filter { it.index % 100 == 0 }
                    .mapNotNull {
                        val y = it.value.toFloat()
                        if (y.isFinite()) Entry(it.index.toFloat(), y) else null
                    }
                if (downsampled.isNotEmpty()) {
                    val dataSet = LineDataSet(downsampled, "").apply {
                        color = lineColor // CHANGED
                        setDrawCircles(false)
                        setDrawValues(false)
                        lineWidth = 1f
                    }
                    try {
                        chart.data = LineData(dataSet)
                        chart.notifyDataSetChanged()
                        chart.invalidate()
                    } catch (e: Exception) {
                        Log.e("ChartError", "Chart update failed: ${e.localizedMessage}")
                    }
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    var showInfo by remember { mutableStateOf(false) }
    var showPilotDialog by remember { mutableStateOf(false) }

    val menuItems = listOf(
        MenuCardItem("Gait Monitor", R.drawable.menu_gait, onClickRoute = "main"),
        MenuCardItem("Voice Analysis", R.drawable.menu_voice, onClickRoute = "voice"),
        MenuCardItem("Glove Control", R.drawable.menu_glove, onClickRoute = null),
        MenuCardItem("Symptom Tracker", R.drawable.menu_symptoms, onClickRoute = null),
        MenuCardItem("Settings", R.drawable.menu_settings, onClickRoute = "settings")
    )


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                BottomAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { showInfo = true }) {
                        Text("Info")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { (context as? Activity)?.finish() }) {
                        Text("Exit")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Española GloveWorks",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuCardGrid(
                items = menuItems,
                columns = 2
            ) { item ->
                if (item.onClickRoute != null) {
                    navController.navigate(item.onClickRoute)
                } else {
                    showPilotDialog = true
                }
            }

        }

        if (showInfo) {
            InfoOverlay(
                title = "Welcome to Espanola GloveWorks",
                infoText = """
            Below is a brief description of the App's functionality:
            
            • Gait Monitor: records your walking movement.
            • Voice Analysis: records a short voice sample.
            • Glove Control: connect to your glove and adjust settings.
            • Symptom Tracker: log symptoms and medication times.
            • Settings: user info and app info.
            
            For Help on any screen, Tap the Info Button for directions.
        """.trimIndent(),
                onDismiss = { showInfo = false }
            )
        }

        if (showPilotDialog) {
            AlertDialog(
                onDismissRequest = { showPilotDialog = false },
                title = { Text("Not Available") },
                text = { Text("This feature is not available in the pilot version of the app.") },
                confirmButton = {
                    Button(onClick = { showPilotDialog = false }) { Text("OK") }
                }
            )
        }
    }
}

data class EpochFeatures(
    val rms: Float,
    val fftData: List<Pair<Float, Float>>,
    val dominantFreq: Float,
    val envelope: List<Float>
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(navController: NavHostController) {
    val context = LocalContext.current
    var showInfo by remember { mutableStateOf(false) }
    val permissionState = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        if (!permissionState.value && context is Activity) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1001
            )

            delay(500)
            permissionState.value = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    val sampleRate = 22050
    val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    var audioRecord by remember { mutableStateOf<AudioRecord?>(null) }
    var isCountingDown by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableStateOf(0) }
    var showPostRunDialog by remember { mutableStateOf(false) }
    var incidentText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var collectedSamples by remember { mutableStateOf(mutableListOf<Short>()) }
    var rmsValues by remember { mutableStateOf(mutableListOf<Float>()) }
    var fftResults by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var epochFreqList by remember { mutableStateOf(mutableListOf<Pair<Int, Float>>()) }
    val downsampledSamples = remember(collectedSamples) {
        if (collectedSamples.size <= 2000) {
            collectedSamples
        } else {
            collectedSamples.filterIndexed { index, _ -> index % 10 == 0 }
        }
    }
    fun calculateRMS(samples: List<Short>): Float {
        if (samples.isEmpty()) return 0f
        val sumSquares = samples.fold(0.0) { acc, s -> acc + s * s }
        return kotlin.math.sqrt(sumSquares / samples.size).toFloat()
    }
    fun startRecording() {
        val sharedPref = context.getSharedPreferences("gloveworks_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().remove("voice_session_id").apply()
        collectedSamples = mutableListOf()
        rmsValues = mutableListOf()
        epochFreqList = mutableListOf()
        fftResults = emptyList()

        coroutineScope.launch {
            // 5 second countdown
            withContext(Dispatchers.Main) { isCountingDown = true }
            for (i in 5 downTo 1) {
                withContext(Dispatchers.Main) { countdownValue = i }
                delay(1000)
            }

            // Beep through earpiece
            withContext(Dispatchers.Main) {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                audioManager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = false

                val sampleRate = 44100
                val duration = 0.6
                val numSamples = (sampleRate * duration).toInt()
                val samples = ShortArray(numSamples)
                for (i in samples.indices) {
                    samples[i] = (Short.MAX_VALUE * Math.sin(2.0 * Math.PI * 880.0 * i / sampleRate)).toInt().toShort()
                }
                val audioTrack = android.media.AudioTrack.Builder()
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        android.media.AudioFormat.Builder()
                            .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(android.media.AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()
                delay(700)
                audioTrack.stop()
                audioTrack.release()
                audioManager.mode = android.media.AudioManager.MODE_NORMAL
            }

            withContext(Dispatchers.Main) {
                isCountingDown = false
                countdownValue = 0
                isRecording = true
                collectedSamples = mutableListOf()
            }

            try {
                val record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    withContext(Dispatchers.Main) { isRecording = false }
                    return@launch
                }
                audioRecord = record
                record.startRecording()
                repeat(4) { epochIndex ->
                    val buffer = ShortArray(bufferSize)
                    val epochSamples = mutableListOf<Short>()
                    val startTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - startTime < 5000 && isRecording) {
                        val read = record.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            epochSamples.addAll(buffer.take(read))
                        } else break
                        delay(100)
                    }
                    val features = withContext(Dispatchers.Default) {
                        val rms = calculateRMS(epochSamples)
                        val fftData = computeFFT(epochSamples, sampleRate)
                        val dominantFreq = fftData.maxByOrNull { it.second }?.first ?: 0f
                        val envelope = epochSamples
                            .chunked(1000)
                            .map { chunk ->
                                val sumSq = chunk.fold(0.0) { acc, s -> acc + s * s }
                                kotlin.math.sqrt(sumSq / chunk.size).toFloat()
                            }
                        EpochFeatures(rms, fftData, dominantFreq, envelope)
                    }
                    withContext(Dispatchers.Main) {
                        collectedSamples.addAll(epochSamples)
                        rmsValues.add(features.rms)
                        epochFreqList.add(Pair(epochIndex + 1, features.dominantFreq))
                    }
                    uploadVoiceEpochToFirebase(
                        context = context,
                        epochIndex = epochIndex + 1,
                        rms = features.rms,
                        dominantFreq = features.dominantFreq,
                        fftData = features.fftData,
                        waveformEnvelope = features.envelope
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    audioRecord?.let {
                        if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) it.stop()
                        it.release()
                    }
                } catch (_: Exception) {}
                audioRecord = null

                withContext(Dispatchers.Main) {
                    isRecording = false
                    showPostRunDialog = true
                }
            }
        }

    }
    fun stopRecording() {
        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                isRecording = false
            }
            try {
                audioRecord?.let {
                    if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        it.stop()
                    }
                    it.release()
                }
            } catch (e: Exception) {
                println("StopRecording error: ${e.message}")
            }
            audioRecord = null
        }
    }
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Voice Analysis") }) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                BottomAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { showInfo = true }) {
                        Text("Info")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { navController.navigate("home") }) {
                        Text("Home")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isRecording) {
                val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
                val pulseSize by infiniteTransition.animateFloat(
                    initialValue = 20f,
                    targetValue = 40f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "PulseSize"
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(40.dp), // reserve max space
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(pulseSize.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }

                }
            }
            if (isCountingDown) {
                Text(
                    text = "Starting in $countdownValue...",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(
                onClick = {
                    if (permissionState.value && !isRecording && !isCountingDown) {
                        startRecording()
                    }
                },
                enabled = !isRecording && !isCountingDown,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start")
            }

            Button(
                onClick = { stopRecording() },
                enabled = isRecording,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop")
            }
            Text("Samples collected: ${collectedSamples.size}")
            AudioWaveformGraph(downsampledSamples, modifier = Modifier.height(150.dp))
            RMSGraph(rmsValues, modifier = Modifier.height(150.dp))
            FFTAudioTrendGraph(epochFreqList, modifier = Modifier.height(150.dp))
        }
    }
    if (showInfo) {
        InfoOverlay(
            title = "Voice Analysis Directions",
            infoText = """
        • Find a quiet room.
        • Hold the phone to your ear like a phone call.
        • Tap Start.
        • Wait for the beep.
        • Take a deep breath and say "Ahhh" in a steady voice.
        • Keep saying "Ahhh" as long as you can.
        • Tap Stop when you can no longer sustain the sound.
        • Tap Home to return.
        """.trimIndent(),
            onDismiss = { showInfo = false }
        )
    }
    if (showPostRunDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Recording Complete") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Any incidents to report? (e.g. interruption, noise, coughing)")
                    OutlinedTextField(
                        value = incidentText,
                        onValueChange = { incidentText = it },
                        label = { Text("Incident notes (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "If the recording did not go as expected, tap \"Discard\" instead of submitting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val sharedPref = context.getSharedPreferences("gloveworks_prefs", Context.MODE_PRIVATE)
                    val participantId = sharedPref.getString("participant_id", "unknown") ?: "unknown"
                    val metadata = mapOf(
                        "participantId" to participantId,
                        "incidents" to incidentText.ifBlank { "none" },
                        "timestamp" to System.currentTimeMillis()
                    )
                    FirebaseDatabase.getInstance().reference
                        .child("participants")
                        .child(participantId)
                        .child("voice_runs")
                        .push()
                        .setValue(metadata)
                    showPostRunDialog = false
                    incidentText = ""
                }) { Text("Submit") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showPostRunDialog = false
                    incidentText = ""
                    Toast.makeText(context, "Recording discarded.", Toast.LENGTH_SHORT).show()
                }) { Text("Discard (recording was unsuccessful)") }
            }
        )
    }

}

fun computeFFT(samples: List<Short>, sampleRate: Int): List<Pair<Float, Float>> {
    val n = samples.size
    val fft = DoubleArray(n * 2) // real + imag
    for (i in samples.indices) {
        fft[2 * i] = samples[i].toDouble()
        fft[2 * i + 1] = 0.0
    }
    val fftInstance = DoubleFFT_1D(n.toLong())
    fftInstance.complexForward(fft)
    val result = mutableListOf<Pair<Float, Float>>()
    val freqResolution = sampleRate.toFloat() / n
    for (i in 0 until n / 2) {
        val real = fft[2 * i]
        val imag = fft[2 * i + 1]
        val magnitude = sqrt(real * real + imag * imag).toFloat()
        val freq = i * freqResolution
        result.add(Pair(freq, magnitude))
    }
    return result
}

@Composable
fun RMSGraph(rmsValues: List<Float>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.secondary.toArgb() // NEW
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(false)
                setScaleEnabled(false)
                setPinchZoom(false)
                isDragEnabled = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setViewPortOffsets(0f, 0f, 0f, 0f)
                xAxis.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    setDrawLabels(false)
                    setDrawAxisLine(false)
                }
                axisLeft.apply {
                    isEnabled = true
                    axisMinimum = 0f
                    setDrawGridLines(false)
                    setDrawLabels(false)
                    setDrawAxisLine(false)
                }
                axisRight.isEnabled = false
                legend.isEnabled = false
            }
        },
        update = { chart ->
            if (rmsValues.isNotEmpty()) {
                val entries = rmsValues.mapIndexed { index, value ->
                    Entry(index.toFloat(), value)
                }
                val dataSet = LineDataSet(entries, "").apply {
                    color = lineColor // CHANGED
                    setDrawCircles(false)
                    circleRadius = 4f
                    setDrawValues(false)
                    lineWidth = 2f
                    mode = LineDataSet.Mode.LINEAR
                }
                chart.data = LineData(dataSet)
                chart.notifyDataSetChanged()
                chart.invalidate()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

@Composable
fun FFTAudioTrendGraph(epochFreqList: List<Pair<Int, Float>>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.tertiary.toArgb() // NEW
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(false)
                setScaleEnabled(false)
                setPinchZoom(false)
                isDragEnabled = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setViewPortOffsets(0f, 0f, 0f, 0f)
                xAxis.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    setDrawLabels(false)
                    setDrawAxisLine(false)
                }
                axisLeft.apply {
                    isEnabled = true
                    axisMinimum = 0f
                    axisMaximum = 40f
                    setDrawGridLines(false)
                    setDrawLabels(false)
                    setDrawAxisLine(false)
                }
                axisRight.isEnabled = false
                legend.isEnabled = false
            }
        },
        update = { chart ->
            if (epochFreqList.isNotEmpty()) {
                val maxY = epochFreqList.maxOfOrNull { it.second }?.coerceAtLeast(20f) ?: 20f
                chart.axisLeft.axisMaximum = maxY
                val entries = epochFreqList.map { Entry(it.first.toFloat(), it.second) }
                val dataSet = LineDataSet(entries, "").apply {
                    color = lineColor
                    setDrawCircles(false)
                    circleRadius = 4f
                    setDrawValues(false)
                    lineWidth = 2f
                    mode = LineDataSet.Mode.LINEAR
                }
                chart.data = LineData(dataSet)
                chart.notifyDataSetChanged()
                chart.invalidate()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

@Composable
fun FFTTrendGraph(epochFreqList: List<Pair<Int, Float>>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.tertiary.toArgb()
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(false)
                setScaleEnabled(false)
                setPinchZoom(false)
                isDragEnabled = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setViewPortOffsets(0f, 0f, 0f, 0f)
                xAxis.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    setDrawLabels(false)
                    setDrawAxisLine(false)
                }
                axisLeft.apply {
                    isEnabled = true
                    axisMinimum = 0f
                    axisMaximum = 16f
                    setDrawLabels(false)
                    setDrawGridLines(false)
                    setDrawAxisLine(false)
                }
                axisRight.isEnabled = false
                legend.isEnabled = false
            }
        },
        update = { chart ->
            if (epochFreqList.isNotEmpty()) {
                val entries = epochFreqList.map { Entry(it.first.toFloat(), it.second) }
                val dataSet = LineDataSet(entries, "").apply {
                    color = lineColor // CHANGED
                    setDrawCircles(false)
                    circleRadius = 4f
                    setDrawValues(false)
                    lineWidth = 3f
                    mode = LineDataSet.Mode.LINEAR
                }
                chart.data = LineData(dataSet)
                chart.notifyDataSetChanged()
                chart.invalidate()
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

data class MotionSample(
    val t: Long,
    val ax: Float, val ay: Float, val az: Float,
    val gx: Float, val gy: Float, val gz: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaitMonitor(navController: NavHostController, userInfoViewModel: UserInfoViewModel) {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    var isCollecting by remember {
        mutableStateOf(false)
    }
    val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    var latestGyro by remember { mutableStateOf(Triple(0f, 0f, 0f)) }
    var sensorDataList by remember {
        mutableStateOf<List<Pair<Float, Float>>>(emptyList())
    }
    var fftDataList by remember {
        mutableStateOf<List<Pair<Float, Float>>>(emptyList())
    }
    var epochFreqList by remember {
        mutableStateOf<List<Pair<Int, Float>>>(emptyList())
    }
    var elapsedTime by remember {
        mutableStateOf(0f)
    }
    var epochSamples by remember {
        mutableStateOf(mutableListOf<MotionSample>())
    }

    var showLocationDialog by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf("") }
    var countdownValue by remember { mutableStateOf(0) }
    var isCountingDown by remember { mutableStateOf(false) }
    var showPostRunDialog by remember { mutableStateOf(false) }
    var incidentText by remember { mutableStateOf("") }
    var pendingDiscard by remember { mutableStateOf(false) }
    var finalScore by remember { mutableStateOf<Double?>(null) }
    var showScoreDialog by remember { mutableStateOf(false) }
    val epochRoughnessList = remember { mutableStateListOf<Double>() }
    var showInfo by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    when (it.sensor.type) {
                        Sensor.TYPE_GYROSCOPE -> {
                            latestGyro = Triple(it.values[0], it.values[1], it.values[2])
                        }
                        Sensor.TYPE_ACCELEROMETER -> {
                            if (isCollecting) {
                                val ax = it.values[0]
                                val ay = it.values[1]
                                val az = it.values[2]
                                val (gx, gy, gz) = latestGyro
                                val magnitude = sqrt(ax * ax + ay * ay + az * az)
                                elapsedTime += 0.1f
                                sensorDataList = sensorDataList + Pair(elapsedTime, magnitude)
                                sensorDataList = sensorDataList.filter { it.first >= elapsedTime - 5f }
                                epochSamples.add(
                                    MotionSample(
                                        t = System.currentTimeMillis(),
                                        ax = ax, ay = ay, az = az,
                                        gx = gx, gy = gy, gz = gz
                                    )
                                )
                            }
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    fun stopCollecting() {
        isCollecting = false
        sensorManager.unregisterListener(sensorListener)
        elapsedTime = 0f
    }
    fun startCollecting() {
        sensorManager.registerListener(
            sensorListener, accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
        sensorManager.registerListener(
            sensorListener, gyroscope,
            SensorManager.SENSOR_DELAY_UI
        )
        showLocationDialog = true
    }

    fun beginCountdown() {
        val sharedPref = context.getSharedPreferences("gloveworks_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().remove("motion_session_id").apply()
        isCountingDown = true
        countdownValue = 10
        sensorDataList = emptyList()
        fftDataList = emptyList()
        epochFreqList = emptyList()
        elapsedTime = 0f
        epochRoughnessList.clear()
        finalScore = null

        coroutineScope.launch {
            // Start beep
            val startTone = android.media.ToneGenerator(
                android.media.AudioManager.STREAM_MUSIC, 100
            )
            startTone.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
            delay(600)
            startTone.release()

            // Countdown
            for (i in 10 downTo 1) {
                countdownValue = i
                delay(1000)
            }
            isCountingDown = false
            countdownValue = 0

            // Start collecting
            isCollecting = true
            sensorManager.registerListener(
                sensorListener, accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )

            for (epochIndex in 1..4) {
                epochSamples = mutableListOf()
                val cycleStartTime = System.currentTimeMillis()
                while (isCollecting) {
                    val elapsedMillis = System.currentTimeMillis() - cycleStartTime
                    if (elapsedMillis >= 5000) break
                    delay(100)
                }
                if (!isCollecting) break

                val roughness = epochRoughnessFromSamples(epochSamples.map { Triple(it.ax, it.ay, it.az) })
                val accelerationValues = sensorDataList.map { it.second }
                val fftResult = computeFFT(accelerationValues)
                fftDataList = fftResult
                val dominantFrequency = fftResult.maxByOrNull { it.second }?.first ?: 0f
                epochFreqList = epochFreqList + Pair(epochIndex, dominantFrequency)
                uploadMotionEpochToFirebase(
                    context = context,
                    epochIndex = epochIndex,
                    accelerometerSamples = epochSamples
                )
            }

            // Stop beep
            val stopTone = android.media.ToneGenerator(
                android.media.AudioManager.STREAM_MUSIC, 100
            )
            stopTone.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE, 800)
            delay(900)
            stopTone.release()

            stopCollecting()
            showPostRunDialog = true
        }
    }


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { CenterAlignedTopAppBar(title = { Text("GloveWorks Gait Monitor") }) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                BottomAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { showInfo = true }) {
                        Text("Info")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { navController.navigate("home") }) {
                        Text("Home")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { startCollecting() },
                    enabled = !isCollecting && !isCountingDown,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                ) { Text("Start") }
                Button(
                    onClick = { stopCollecting() },
                    enabled = isCollecting,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                ) { Text("Stop") }
            }

// Countdown display
            if (isCountingDown) {
                Text(
                    text = "Starting in $countdownValue...",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 16.dp),
//                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
//            ) {
//                Column(modifier = Modifier.padding(16.dp)) {
//                    Text("Gait Score", style = MaterialTheme.typography.titleLarge)
//                    Text(
//                        text = finalScore?.let { String.format("%.1f / 10", it) } ?: "—",
//                        style = MaterialTheme.typography.headlineMedium
//                    )
//                    Text(
//                        text = "Computed after 4 epochs",
//                        style = MaterialTheme.typography.bodySmall
//                    )
//                }
//            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RealTimeGraph(sensorDataList, modifier = Modifier.weight(1f))
                FFTGraph(fftDataList, modifier = Modifier.weight(1f))
                FFTTrendGraph(epochFreqList, modifier = Modifier.weight(1f))
            }
        }
//        if (showScoreDialog && finalScore != null) {
//            AlertDialog(
//                onDismissRequest = { showScoreDialog = false },
//                title = { Text("Gait Score") },
//                text = { Text("Your gait score is ${String.format("%.1f", finalScore)} / 10") },
//                confirmButton = {
//                    Button(onClick = { showScoreDialog = false }) { Text("OK") }
//                }
//            )
//        }
    }
    if (showInfo) {
        InfoOverlay(
            title = "Gait Monitor Directions",
            infoText = """
        •Find a safe, open space.
        • Put the phone in a pocket(Use Same Pocket Every Time).
        • Tap Start.
        • Walk straight for about 20 seconds.
        • Tap Stop anytime.
        • Not enough room? Turn around and keep walking.
        • No straight path? Walk in a wide circle.
        """.trimIndent(),
            onDismiss = { showInfo = false }
        )
    }
    // Location selection dialog
    if (showLocationDialog) {
        val locations = listOf("Pocket", "Handheld", "Waist", "Upper shin")
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Phone Location") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Where is the phone placed?")
                    locations.forEach { location ->
                        OutlinedButton(
                            onClick = {
                                selectedLocation = location
                                showLocationDialog = false
                                beginCountdown()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedLocation == location)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                        ) {
                            Text(location)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) { Text("Cancel") }
            }
        )
    }

// Post run dialog
    if (showPostRunDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Run Complete") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Location: $selectedLocation")
                    Text("Any incidents to report? (e.g. freezing of gait, tremors, fall, interruption)")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("If the run did not go as expected, tap \"Discard Run\" instead of submitting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = incidentText,
                        onValueChange = { incidentText = it },
                        label = { Text("Incident notes (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Upload run metadata to Firebase
                    val sharedPref = context.getSharedPreferences("gloveworks_prefs", Context.MODE_PRIVATE)
                    val participantId = sharedPref.getString("participant_id", "unknown") ?: "unknown"
                    val metadata = mapOf(
                        "participantId" to participantId,
                        "phoneLocation" to selectedLocation,
                        "incidents" to incidentText.ifBlank { "none" },
                        "timestamp" to System.currentTimeMillis()
                    )
                    FirebaseDatabase.getInstance().reference
                        .child("participants")
                        .child(participantId)
                        .child("runs")
                        .push()
                        .setValue(metadata)
                    showPostRunDialog = false
                    incidentText = ""
                }) { Text("Submit") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showPostRunDialog = false
                    incidentText = ""
                    Toast.makeText(context, "Run discarded.", Toast.LENGTH_SHORT).show()
                }) { Text("Discard Run") }
            }
        )
    }
}

private fun median(values: List<Double>): Double {
    if (values.isEmpty()) return 0.0
    val sorted = values.sorted()
    val n = sorted.size
    return if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
}
private fun mad(values: List<Double>): Double {
    // median(|y - median(y)|)
    if (values.isEmpty()) return 0.0
    val m = median(values)
    val deviations = values.map { kotlin.math.abs(it - m) }
    return median(deviations)
}
private fun epochRoughnessFromSamples(epochSamples: List<Triple<Float, Float, Float>>): Double {
    if (epochSamples.size < 2) return 0.0
    // y_i = magnitude
    val y = epochSamples.map { (x, yy, z) ->
        sqrt((x * x + yy * yy + z * z).toDouble())
    }
    // change_i = |y_i - y_{i-1}|
    val changes = mutableListOf<Double>()
    for (i in 1 until y.size) {
        changes.add(abs(y[i] - y[i - 1]))
    }
    val typicalSize = mad(y).coerceAtLeast(1e-9) // avoid divide-by-zero
    val changeMedian = median(changes)
    return changeMedian / typicalSize
}

private fun scoreFromR(R: Double, alpha: Double = 0.8): Double {
    // score = 1 + 9*exp(-alpha*R)
    val s = 1.0 + 9.0 * exp(-alpha * R)
    return s.coerceIn(1.0, 10.0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var optedIn by remember { mutableStateOf(isResearchOptedIn(context)) }
    val userId = remember(optedIn) { if (optedIn) getUserId(context) else null }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { CenterAlignedTopAppBar(title = { Text("Settings") }) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                BottomAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { navController.navigate("home") }) {
                        Text("Home")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //Research Opt-In
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Opt-In to Research",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Allows anonymous syncing of your data to the research database.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = optedIn,
                            onCheckedChange = { enabled ->
                                optedIn = enabled
                                setResearchOptIn(context, enabled)

                                if (enabled) {
                                    // Ensure user_id exists when opting in
                                    val id = getUserId(context) ?: generateUserId(isAnonymous = true)
                                    setUserId(context, id)
                                    // Immediately sync down from Firebase
                                    scope.launch {
                                        val count = SyncManager.syncAllFromFirebase(context)
                                        Toast.makeText(
                                            context,
                                            "Opted in. Synced $count entries.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Opted out. Data will stay local.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    if (optedIn) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Research ID: ${userId ?: "Generating..."}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            if (optedIn) {
                Button(
                    onClick = {
                        scope.launch {
                            val count = SyncManager.syncAllFromFirebase(context)
                            Toast.makeText(
                                context,
                                "Synced $count entries",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Text("Sync Now", fontSize = 24.sp)
                }
            }
            Button(
                onClick = { navController.navigate("user_info") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("User Information", fontSize = 24.sp)
            }
            Button(
                onClick = { navController.navigate("about") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("About", fontSize = 24.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavHostController) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("About", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                )
                {
                    Text("GloveWorks App", style = MaterialTheme.typography.titleMedium)
                    Text("Version: 1.0.0")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Developed by:")
                    Text("Ali Nazım Aslan", style = MaterialTheme.typography.bodyMedium)
                    Text("Niel Kirby Salero", style = MaterialTheme.typography.bodyMedium)
                    Text("Nicholas Lopez", style = MaterialTheme.typography.bodyMedium)
                    Text("Emily Schutz", style = MaterialTheme.typography.bodyMedium)
                    Text("for Española GloveWorks")
                    Text(
                        "at Northern New Mexico College, 2025",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Learn more at:")
                    Spacer(modifier = Modifier.height(2.dp))
                    Button(onClick = {
                        val url = "https://sites.google.com/nnmc.edu/espanolagloveworks/home"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }) {
                        Text("GloveWorks Website")
                    }
                }
            }
        }
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .padding(bottom = 24.dp)
        ) {
            Text("Back")
        }
    }
}
class GloveBleController(
    private val context: Context,
    private val serviceUuid: UUID
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner: BluetoothLeScanner? get() = adapter?.bluetoothLeScanner
    private var gatt: BluetoothGatt? = null
    private var connectedDevice: BluetoothDevice? = null
    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    private val _statusText = MutableStateFlow("Disconnected")
    val statusText: StateFlow<String> = _statusText
    fun ensureBlePermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val need = mutableListOf<String>()
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED
            ) need.add(Manifest.permission.BLUETOOTH_SCAN)
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) need.add(Manifest.permission.BLUETOOTH_CONNECT)
            if (need.isNotEmpty()) {
                ActivityCompat.requestPermissions(activity, need.toTypedArray(), 7001)
            }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    7002
                )
            }
        }
    }
    @SuppressLint("MissingPermission")
    fun startScan() {
        _statusText.value = "Scanning..."
        _devices.value = emptyList()
        val s = scanner ?: run {
            _statusText.value = "Bluetooth not available"
            return
        }
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(serviceUuid))
                .build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        s.startScan(filters, settings, scanCallback)
    }
    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanner?.stopScan(scanCallback)
        if (!_isConnected.value) _statusText.value = "Disconnected"
    }
    @SuppressLint("MissingPermission")
    fun connectByIndex(index: Int) {
        val list = _devices.value
        if (index !in list.indices) return
        stopScan()
        val device = list[index]
        connect(device)
    }
    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        connectedDevice = device
        _statusText.value = "Connecting..."
        gatt?.close()
        gatt = device.connectGatt(context, false, gattCallback)
    }
    @SuppressLint("MissingPermission")
    fun disconnect() {
        stopScan()
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        connectedDevice = null
        _isConnected.value = false
        _statusText.value = "Disconnected"
    }
    @SuppressLint("MissingPermission")
    fun writeIntAsString(characteristicUuid: UUID, value: Int) {
        val g = gatt ?: return
        val service = g.getService(serviceUuid) ?: return
        val ch = service.getCharacteristic(characteristicUuid) ?: return
        ch.value = value.toString().toByteArray(Charset.forName("UTF-8"))
        ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        g.writeCharacteristic(ch)
    }
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val current = _devices.value
            if (current.none { it.address == device.address }) {
                _devices.value = current + device
            }
        }
        override fun onScanFailed(errorCode: Int) {
            _statusText.value = "Scan failed: $errorCode"
        }
    }
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _statusText.value = "Connected"
                _isConnected.value = true
                gatt.discoverServices()
            } else {
                _isConnected.value = false
                _statusText.value = "Disconnected"
                try {
                    gatt.close()
                } catch (_: Exception) {
                }
            }
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GloveControlScreen(navController: NavHostController) {
    val context = LocalContext.current
    var showInfo by remember { mutableStateOf(false) }
    val activity = context as? Activity
    val prefs = remember {
        context.getSharedPreferences("gloveworks_prefs", Context.MODE_PRIVATE)
    }
    // PIN LOCK
    // Default PIN is 1234
    val storedPin = remember { prefs.getString("glove_control_pin", "1234") ?: "1234" }
    var isUnlocked by rememberSaveable { mutableStateOf(false) }
    var showPinDialog by rememberSaveable { mutableStateOf(true) }
    var pinInput by rememberSaveable { mutableStateOf("") }
    var pinError by rememberSaveable { mutableStateOf<String?>(null) }
    if (!isUnlocked && showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                navController.popBackStack()
            },
            title = { Text("Enter PIN") },
            text = {
                Column {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it.filter { ch -> ch.isDigit() }.take(8)
                            pinError = null
                        },
                        label = { Text("PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = pinError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput == storedPin) {
                            isUnlocked = true
                            showPinDialog = false
                            pinInput = ""
                            pinError = null
                        } else {
                            pinError = "Incorrect PIN"
                        }
                    }
                ) { Text("Unlock") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPinDialog = false
                        navController.popBackStack()
                    }
                ) { Text("Cancel") }
            }
        )
    }
    if (!isUnlocked) return
    val SERVICE_UUID = remember { UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b") }
    val CHAR_MOTOR_AMP =
        remember { UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8") }     // Char1
    val CHAR_PATTERN_DUR =
        remember { UUID.fromString("e3223119-9445-4e96-a4a1-85358c4046a2") }   // Char2
    val CHAR_BURST_DUR =
        remember { UUID.fromString("e3223119-9445-4e96-a4a1-85358c4046a3") }     // Char3
    val defaultMA = 100
    val defaultPD = 744
    val defaultBD = 100
    // Slider limits
    val MA_RANGE = 50..100          // percent
    val PD_RANGE = 500..1000        // ms
    val BD_RANGE = 50..150          // ms
    var motorAmp by remember { mutableIntStateOf(prefs.getInt("MAval", defaultMA)) }
    var patternDur by remember { mutableIntStateOf(prefs.getInt("PDval", defaultPD)) }
    var burstDur by remember { mutableIntStateOf(prefs.getInt("BDval", defaultBD)) }
    val bleController = remember { GloveBleController(context, SERVICE_UUID) }
    val devices by bleController.devices.collectAsState()
    val isConnected by bleController.isConnected.collectAsState()
    val statusText by bleController.statusText.collectAsState()
    LaunchedEffect(Unit) {
        if (activity != null) {
            bleController.ensureBlePermissions(activity)
        }
    }
    fun persistValues() {
        prefs.edit()
            .putInt("MAval", motorAmp)
            .putInt("PDval", patternDur)
            .putInt("BDval", burstDur)
            .apply()
    }
    fun coerceAllToValidRanges() {
        motorAmp = motorAmp.coerceIn(MA_RANGE)
        burstDur = burstDur.coerceIn(BD_RANGE)

        val minPd = (4 * burstDur).coerceIn(PD_RANGE)
        patternDur = patternDur.coerceIn(minPd..PD_RANGE.last)
    }
    LaunchedEffect(Unit) {
        coerceAllToValidRanges()
        persistValues()
    }
    fun sendAllIfConnected() {
        if (!isConnected) return
        bleController.writeIntAsString(CHAR_MOTOR_AMP, motorAmp)
        bleController.writeIntAsString(CHAR_PATTERN_DUR, patternDur)
        bleController.writeIntAsString(CHAR_BURST_DUR, burstDur)
    }
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Glove Control") }) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                BottomAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { showInfo = true }) {
                        Text("Info")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { navController.navigate("home") }) {
                        Text("Home")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Scan for your glove, connect, then tune the three parameters below.",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { bleController.startScan() },
                    modifier = Modifier.weight(1f)
                ) { Text("Start Scanning") }
                Button(
                    onClick = { bleController.disconnect() },
                    enabled = isConnected,
                    modifier = Modifier.weight(1f)
                ) { Text("Disconnect") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("BLE Status:")
                Text(statusText)
            }
            if (devices.isNotEmpty() && !isConnected) {
                Text("Devices Found:", style = MaterialTheme.typography.titleSmall)
                Card {
                    Column(modifier = Modifier.padding(8.dp)) {
                        devices.forEachIndexed { index, device ->
                            val label = buildString {
                                append(device.name ?: "Unnamed Device")
                                append("  •  ")
                                append(device.address)
                            }
                            OutlinedButton(
                                onClick = { bleController.connectByIndex(index) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            ParameterSlider(
                title = "Motor Amplitude",
                value = motorAmp,
                valueRange = MA_RANGE,
                enabled = isConnected,
                onValueChanged = { newVal ->
                    motorAmp = newVal.coerceIn(MA_RANGE)
                    persistValues()
                    bleController.writeIntAsString(CHAR_MOTOR_AMP, motorAmp)
                }
            )
            ParameterSlider(
                title = "Burst Duration",
                value = burstDur,
                valueRange = BD_RANGE,
                enabled = isConnected,
                onValueChanged = { newVal ->
                    burstDur = newVal.coerceIn(BD_RANGE)
                    val minPd = (4 * burstDur).coerceIn(PD_RANGE)
                    if (patternDur < minPd) {
                        patternDur = minPd
                        if (isConnected) {
                            bleController.writeIntAsString(CHAR_PATTERN_DUR, patternDur)
                        }
                    }
                    persistValues()
                    bleController.writeIntAsString(CHAR_BURST_DUR, burstDur)
                }
            )
            ParameterSlider(
                title = "Pattern Duration",
                value = patternDur,
                valueRange = PD_RANGE,
                enabled = isConnected,
                onValueChanged = { newVal ->
                    val minPd = (4 * burstDur).coerceIn(PD_RANGE)
                    patternDur = newVal.coerceIn(minPd..PD_RANGE.last)

                    persistValues()
                    bleController.writeIntAsString(CHAR_PATTERN_DUR, patternDur)
                }
            )
            if (isConnected) {
                OutlinedButton(
                    onClick = { sendAllIfConnected() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Re-Send All Values")
                }
            }
        }
    }
    if (showInfo) {
        InfoOverlay(
            title = "Glove Control Directions",
            infoText = """
        • Ensure that your phone's bluetooth and location are enabled
        • Turn on the glove.
        • Tap Start Scanning.
        • Allow permission for the app to scan for Bluetooth Devices
        • Tap your glove name to connect.
        • Use the 3 sliders to adjust the glove:
          - Motor Amplitude: strength
          - Burst Duration: pulse length
          - Pattern Duration: time between pulses
        • When finished, tap Disconnect.
        • Tap Home to return.
        """.trimIndent(),
            onDismiss = { showInfo = false }
        )
    }
}

@Composable
fun ParameterSlider(
    title: String,
    value: Int,
    valueRange: IntRange,
    enabled: Boolean,
    onValueChanged: (Int) -> Unit,
    unit: String? = null,
    helperText: String? = null,
) {
    val displayValue = if (unit.isNullOrBlank()) "$value" else "$value $unit"
    val disabledAlpha = if (enabled) 1f else 0.45f
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(disabledAlpha),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!helperText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = helperText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                AssistChip(
                    onClick = { /* no-op */ },
                    enabled = false,
                    label = {
                        Text(
                            text = displayValue,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { newFloat ->
                    val newInt = newFloat.roundToInt().coerceIn(valueRange)
                    onValueChanged(newInt)
                },
                valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
                enabled = enabled
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = valueRange.first.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = valueRange.last.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomTrackerScreen(navController: NavHostController) {
    val menuItems = listOf(
        MenuCardItem("Medication", R.drawable.menu_medicine, onClickRoute = "symptom_medication"),
        MenuCardItem("Lifestyle", R.drawable.menu_lifestyle, onClickRoute = "symptom_lifestyle"),
        MenuCardItem("Physical", R.drawable.menu_physical, onClickRoute = "symptom_physical"),
        MenuCardItem("Psychological", R.drawable.menu_psychological, onClickRoute = "symptom_psychological")
    )
    var showInfo by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                BottomAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { showInfo = true }) {
                        Text("Info")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { navController.navigate("home") }) {
                        Text("Home")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Symptom Tracker",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            MenuCardGrid(
                items = menuItems,
                columns = 2
            ) { item ->
                item.onClickRoute?.let { navController.navigate(it) }
            }
        }
    }
    if (showInfo) {
        InfoOverlay(
            title = "Symptom Tracker Directions",
            infoText = """
        • Choose a category
            Medication
            Lifestyle
            Physical
            Psychological
        • Answer compared to last week
        • Update medications if anything changed
        • Tap Submit
        """.trimIndent(),
            onDismiss = { showInfo = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomQuestionScreen(
    title: String,
    questions: List<String>,
    navController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showInfo by remember { mutableStateOf(false) }
    val responses = remember {
        mutableStateListOf<Int>().apply { repeat(questions.size) { add(0) } }
    }
    val scaleLines = listOf(
        "1 = Significantly Worse",
        "2 = Slightly Worse",
        "3 = No change",
        "4 = Slightly Better",
        "5 = Significantly Better"
    )
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(title) }) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                BottomAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { showInfo = true }) {
                        Text("Info")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { navController.popBackStack() }) { Text("Back") }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "How to answer",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Answer each question based on how you felt over the past week.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    scaleLines.forEach { line ->
                        Text(text = "• $line", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            questions.forEachIndexed { index, question ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(question, style = MaterialTheme.typography.bodyLarge)

                        // Numeric 1..5 options (single row)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            (1..5).forEach { score ->
                                OutlinedButton(
                                    onClick = { responses[index] = score },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor =
                                        if (responses[index] == score)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        else Color.Transparent
                                    )
                                ) {
                                    Text(score.toString())
                                }

                                if (score != 5) Spacer(modifier = Modifier.width(6.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val allAnswered = responses.all { it in 1..5 }
            Button(
                onClick = {
                    val answersMap: Map<String, Any?> = questions.mapIndexed { idx, q ->
                        val value = responses[idx]
                        q to (value.takeIf { it in 1..5 })
                    }.toMap()
                    val answersJson = JSONObject(answersMap).toString()
                    scope.launch {
                        saveSymptomSubmissionLocal(
                            context = context,
                            category = title,
                            answersJson = answersJson
                        )
                        if (isResearchOptedIn(context)) {
                            saveSymptomAnswersToFirebase(
                                context = context,
                                category = title.lowercase(),
                                answers = answersMap
                            ) { success ->
                                Toast.makeText(
                                    context,
                                    if (success) "Saved locally + uploaded for research"
                                    else "Saved locally (upload failed)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(context, "Saved locally", Toast.LENGTH_SHORT).show()
                        }
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = allAnswered
            ) {
                Text(if (allAnswered) "Submit" else "Answer all questions")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    if (showInfo) {
        InfoOverlay(
            title = "Symptom Questions Directions",
            infoText = buildString {
                appendLine("• Read each question carefully")
                appendLine("• Answer each question based on how you felt over the past week")
                appendLine()
                scaleLines.forEach { appendLine(it) }
            },
            onDismiss = { showInfo = false }
        )
    }
}

fun lifestyleQuestions(): List<String> = listOf(
    "Difficulty maintaining a regular sleep schedule?",
    "Changes in appetite or eating habits?",
    "Difficulty keeping a consistent daily routine?",
    "Low energy during the day?",
    "Reduced participation in hobbies or activities?",
    "Difficulty managing stress?",
    "Changes in social engagement?"
)

fun physicalQuestions(): List<String> = listOf(
    "Muscle stiffness or rigidity?",
    "Difficulty with balance?",
    "Slowness of movement?",
    "Shakiness or tremor?",
    "Fatigue during physical activity?",
    "Difficulty standing up from sitting?",
    "Problems with coordination?"
)

fun psychologicalQuestions(): List<String> = listOf(
    "Feeling anxious or worried?",
    "Feeling depressed or sad?",
    "Difficulty concentrating?",
    "Feeling overwhelmed?",
    "Loss of motivation?",
    "Mood swings or irritability?",
    "Feeling mentally fatigued?"
)

data class MedOption(
    val name: String,
    val dosage: String,
    val frequency: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showInfo by remember { mutableStateOf(false) }
    var showGreatJobDialog by remember { mutableStateOf(false) }
    var medExpanded by remember { mutableStateOf(false) }
    var selectedMed by remember { mutableStateOf<MedOption?>(null) }
    var freqExpanded by remember { mutableStateOf(false) }
    val frequencyOptions = remember {
        listOf(
            "Once daily",
            "2x daily",
            "3x daily",
            "4x daily",
            "Every 4 hours",
            "Every 6 hours",
            "Every 8 hours",
            "At bedtime",
            "As needed"
        )
    }
    var selectedFrequency by remember { mutableStateOf(frequencyOptions.first()) }
    var dosageInput by remember { mutableStateOf("") }
    val medOptions = remember {
        listOf(
            MedOption("Carbidopa/Levodopa (Sinemet)", "", ""),
            MedOption("Pramipexole (Mirapex)", "", ""),
            MedOption("Ropinirole (Requip)", "", ""),
            MedOption("Rotigotine (Neupro patch)", "", ""),
            MedOption("Apomorphine (Apokyn)", "", ""),
            MedOption("Rasagiline (Azilect)", "", ""),
            MedOption("Selegiline (Eldepryl/Zelapar)", "", ""),
            MedOption("Safinamide (Xadago)", "", ""),
            MedOption("Entacapone (Comtan)", "", ""),
            MedOption("Opicapone (Ongentys)", "", ""),
            MedOption("Benztropine (Cogentin)", "", ""),
            MedOption("Trihexyphenidyl (Artane)", "", ""),
            MedOption("Amantadine (Symmetrel)", "", ""),
            MedOption("Amantadine ER (Gocovri)", "", "")
        )
    }

    fun resetInputs() {
        selectedMed = null
        dosageInput = ""
        selectedFrequency = frequencyOptions.first()
    }

    fun buildLocalJson(entry: MedOption): String {
        return org.json.JSONArray().apply {
            put(
                org.json.JSONObject().apply {
                    put("name", entry.name)
                    put("dosage", entry.dosage)
                    put("frequency", entry.frequency)
                }
            )
        }.toString()
    }

    fun buildFirebaseAnswers(entry: MedOption): Map<String, Any> {
        return mapOf(
            "count" to 1,
            "medications" to mapOf(
                "0" to mapOf(
                    "name" to entry.name,
                    "dosage" to entry.dosage,
                    "frequency" to entry.frequency
                )
            )
        )
    }

    fun onConfirm() {
        val med = selectedMed
        if (med == null) {
            Toast.makeText(context, "Please select a medication.", Toast.LENGTH_SHORT).show()
            return
        }
        val entry = MedOption(
            name = med.name,
            dosage = dosageInput.trim().ifBlank { "(not set)" },
            frequency = selectedFrequency.trim().ifBlank { "(not set)" }
        )
        val localJson = buildLocalJson(entry)
        val answers = buildFirebaseAnswers(entry)

        scope.launch {
            saveMedicationHistoryLocal(context, localJson)
            showGreatJobDialog = true
            if (isResearchOptedIn(context)) {
                saveSymptomAnswersToFirebase(
                    context = context,
                    category = "medication",
                    answers = answers
                ) { success ->
                    Toast.makeText(
                        context,
                        if (success) "Uploaded for research"
                        else "Upload failed (saved locally)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(context, "Saved locally", Toast.LENGTH_SHORT).show()
            }
            resetInputs()
        }
    }
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Medications") }) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                BottomAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { showInfo = true }) {
                        Text("Info")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { navController.popBackStack() }) { Text("Back") }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Select a medication, then enter your dosage and frequency.")
            // Medication dropdown
            ExposedDropdownMenuBox(
                expanded = medExpanded,
                onExpandedChange = { medExpanded = !medExpanded }
            ) {
                OutlinedTextField(
                    value = selectedMed?.name.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Medication") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = medExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = medExpanded,
                    onDismissRequest = { medExpanded = false }
                ) {
                    medOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name) },
                            onClick = {
                                selectedMed = option
                                dosageInput = ""
                                selectedFrequency = frequencyOptions.first()
                                medExpanded = false
                            }
                        )
                    }
                }
            }
            // Dosage
            OutlinedTextField(
                value = dosageInput,
                onValueChange = { dosageInput = it },
                label = { Text("Dosage") },
                placeholder = { Text("e.g., 25/100 mg or 2 mg") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            // Frequency dropdown
            ExposedDropdownMenuBox(
                expanded = freqExpanded,
                onExpandedChange = { freqExpanded = !freqExpanded }
            ) {
                OutlinedTextField(
                    value = selectedFrequency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = freqExpanded,
                    onDismissRequest = { freqExpanded = false }
                ) {
                    frequencyOptions.forEach { freq ->
                        DropdownMenuItem(
                            text = { Text(freq) },
                            onClick = {
                                selectedFrequency = freq
                                freqExpanded = false
                            }
                        )
                    }
                }
            }
            Button(
                onClick = { onConfirm() },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedMed != null
            ) {
                Text("Confirm")
            }
            Button(
                onClick = { navController.navigate("Medication_history") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Medication History")
            }
        }
    }
    if (showGreatJobDialog) {
        AlertDialog(
            onDismissRequest = { showGreatJobDialog = false },
            title = { Text("Great Job!") },
            text = { Text("Medication selection saved.") },
            confirmButton = {
                Button(onClick = { showGreatJobDialog = false }) { Text("OK") }
            }
        )
    }
    if (showInfo) {
        InfoOverlay(
            title = "Medication Screen Directions",
            infoText = """
                • Tap "Select Medication" and choose your medication
                • Enter your dosage (in mg)
                • Choose your frequency
                • Tap "Confirm" to save (local) and upload if opted in
                • Tap "Medication History" to view past submissions
            """.trimIndent(),
            onDismiss = { showInfo = false }
        )
    }
}

private const val MEDS_PREFS_NAME = "gloveworks_prefs"
private const val KEY_SELECTED_MEDS = "selected_meds_today_v1"
private fun saveSelectedMeds(context: Context, meds: List<MedOption>) {
    val arr = JSONArray()
    meds.forEach { med ->
        val obj = JSONObject().apply {
            put("name", med.name)
            put("dosage", med.dosage)
            put("frequency", med.frequency)
        }
        arr.put(obj)
    }
    context.getSharedPreferences(MEDS_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_SELECTED_MEDS, arr.toString())
        .apply()
}

private fun loadSelectedMeds(context: Context): List<MedOption> {
    val prefs = context.getSharedPreferences(MEDS_PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_SELECTED_MEDS, null) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(
                    MedOption(
                        name = obj.optString("name"),
                        dosage = obj.optString("dosage"),
                        frequency = obj.optString("frequency")
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}
private fun clearSelectedMeds(context: Context) {
    context.getSharedPreferences(MEDS_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove(KEY_SELECTED_MEDS)
        .apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsHistoryScreen(navController: NavHostController) {
    val context = LocalContext.current
    var showInfo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val entities by observeMedicationHistoryLocal(context).collectAsState(initial = emptyList())
    val history = remember(entities) {
        entities.map { e ->
            MedicationHistoryEntry(
                timestamp = e.timestamp,
                medications = medsFromJson(e.medsJson)
            )
        }
    }
    var showClearWarning by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Medication History") }) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                BottomAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { showInfo = true }) {
                        Text("Info")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { navController.popBackStack() }) { Text("Back") }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // --- Clear button (only show if there's something to clear) ---
            if (history.isNotEmpty()) {
                Button(
                    onClick = { showClearWarning = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Medication History")
                }
            }

            if (history.isEmpty()) {
                Text("No medication history yet.")
                Text("Go to Medications and press Confirm to save an entry.")
            } else {
                history.forEach { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatTimestamp(entry.timestamp),
                                style = MaterialTheme.typography.titleMedium
                            )

                            if (entry.medications.isEmpty()) {
                                Text("No medications saved in this entry.")
                            } else {
                                entry.medications.forEach { med ->
                                    Text("• ${med.name}")
                                    Text("  ${med.dosage} • ${med.frequency}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showInfo) {
        InfoOverlay(
            title = "Medication History Directions",
            infoText = """
            • All submitted medications will appear here
            • Tap on "Clear Medication History" to clear the list
            • If you are a research participant, go to settings screen and press "Sync Now"
              to view your medication history from our database
        """.trimIndent(),
            onDismiss = { showInfo = false }
        )
    }
    if (showClearWarning) {
        AlertDialog(
            onDismissRequest = { showClearWarning = false },
            title = { Text("Clear medication history?") },
            text = {
                Text(
                    "This will permanently delete all saved medication history on this device. " +
                            "This cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearWarning = false
                        scope.launch {
                            clearMedicationHistoryLocal(context)
                            Toast.makeText(context, "Medication history cleared.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                Button(onClick = { showClearWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

data class MedicationHistoryEntry(
    val timestamp: Long,
    val medications: List<MedOption>
)

private fun formatTimestamp(ms: Long): String {
    return try {
        val sdf = java.text.SimpleDateFormat("MMM d, yyyy • h:mm a", java.util.Locale.getDefault())
        sdf.format(java.util.Date(ms))
    } catch (e: Exception) {
        "Saved entry"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(navController: NavHostController) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("gloveworks_prefs", Context.MODE_PRIVATE)
    val savedId = remember { sharedPref.getString("participant_id", null) }
    var participantId by remember { mutableStateOf("") }
    var confirmId by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Participant Login") }) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                BottomAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { navController.popBackStack() }) { Text("Back") }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (savedId != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Current Participant ID", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(savedId, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("To change your ID, enter a new one below.",
                    style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Please enter your Participant ID provided by the researcher.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center)
            }

            OutlinedTextField(
                value = participantId,
                onValueChange = { participantId = it.trim(); errorMessage = null },
                label = { Text("Participant ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmId,
                onValueChange = { confirmId = it.trim(); errorMessage = null },
                label = { Text("Confirm Participant ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    when {
                        participantId.isBlank() -> errorMessage = "Please enter your Participant ID."
                        participantId != confirmId -> errorMessage = "IDs do not match. Please try again."
                        else -> {
                            sharedPref.edit()
                                .putString("participant_id", participantId)
                                .apply()
                            showSuccess = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { showSuccess = false },
            title = { Text("Saved!") },
            text = { Text("Participant ID has been saved successfully.") },
            confirmButton = {
                Button(onClick = {
                    showSuccess = false
                    navController.popBackStack()
                }) { Text("OK") }
            }
        )
    }
}


@Composable
fun InfoOverlay(
    title: String,
    infoText: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onDismiss()
            }
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .heightIn(max = 600.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {},
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = infoText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}