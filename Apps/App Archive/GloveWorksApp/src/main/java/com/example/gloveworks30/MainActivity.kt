package com.example.gloveworks30

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.gloveworks30.ui.theme.Gloveworks30Theme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.pow
import org.jtransforms.fft.DoubleFFT_1D
import kotlin.math.sqrt
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.database.FirebaseDatabase
import androidx.lifecycle.viewmodel.compose.viewModel
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.Manifest
import androidx.core.content.ContextCompat
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.rememberInfiniteTransition
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import android.util.Log
import android.net.Uri



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)


        setContent {
            Gloveworks30Theme {
                val navController = rememberNavController()
                val userInfoViewModel: UserInfoViewModel = viewModel()

                val context = LocalContext.current
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val sharedPref = context.getSharedPreferences("gloveworks_prefs",
                        Context.MODE_PRIVATE)
                    val userId = sharedPref.getString("user_id", null)

                    startDestination = if (userId == null) "onboarding" else "home"
                }

                if (startDestination != null) {
                    NavHost(navController = navController, startDestination = startDestination!!) {
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
                        composable("main") { MainScreen(navController, userInfoViewModel) }
                        composable("voice") { VoiceScreen(navController) }
                        composable("settings") { SettingsScreen(navController) }
                        composable("user_info") { UserInfoScreen(navController) }
                        composable("glove_control") { GloveControlScreen(navController) }
                        composable("about") { AboutScreen(navController)
                        }
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
                        // Reset all form state
                        firstName = ""
                        lastName = ""
                        email = ""
                        dob = ""
                        phone = ""
                        pdqResponses.forEachIndexed { index, _ -> pdqResponses[index] = 0 }

                        //pdqResponses.replaceAll { 0 }

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
            }

            else if (isGloveWorksMember == true) {
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
                    Text("GloveWorks Participant Info", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(value = firstName,
                        onValueChange = { firstName = it }, label = { Text("First Name") })
                    OutlinedTextField(value = lastName,
                        onValueChange = { lastName = it }, label = { Text("Last Name") })
                    OutlinedTextField(value = email,
                        onValueChange = { email = it }, label = { Text("Email") })
                    OutlinedTextField(value = dob,
                        onValueChange = { dob = it }, label = { Text("Date of Birth, please use the MM/DD/YYYY format.") })
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
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val userId = generateUserId(
                                isAnonymous = false,
                                firstName=firstName.lowercase(),
                                lastName=lastName.lowercase(),
                                dob=dob)

                            val sharedPref = context.getSharedPreferences("gloveworks_prefs", Context.MODE_PRIVATE)
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

                Text("Please click buttons below to choose the type of anonymous" +
                        " data you would like to share.")
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

                    val sharedPref = context.getSharedPreferences("gloveworks_prefs",
                        Context.MODE_PRIVATE)
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

                // ❌ Axis Cleanup
                xAxis.apply {
                    isEnabled = false
                    setDrawLabels(false)
                    setDrawGridLines(false)
                    setDrawAxisLine(false)
                }
                axisLeft.apply {
                    isEnabled = true  // Must be enabled to enforce limits
                    axisMinimum = 0f
                    axisMaximum = 40f  // Or any fixed value you want
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
                val minTime = sensorData.minOf { it.first }
                val maxTime = sensorData.maxOf { it.first }

                val entries = sensorData.map { (time, magnitude) -> Entry(time, magnitude) }

                val dataSet = LineDataSet(entries, "").apply {
                    color = android.graphics.Color.BLUE
                    setDrawCircles(false)
                    setDrawValues(false)
                    lineWidth = 4f
                    mode = LineDataSet.Mode.LINEAR  // ✅ Smooth connection
                }

                chart.data = LineData(dataSet)

                // Optional: clamp x-range if needed
                chart.xAxis.axisMinimum = maxTime - 5f
                chart.xAxis.axisMaximum = maxTime

                chart.notifyDataSetChanged()
                chart.invalidate()
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

// Original RealTimeGraph

//@Composable
//fun RealTimeGraph(sensorData: List<Pair<Float, Float>>, modifier: Modifier = Modifier) {
//    AndroidView(
//        factory = { context ->
//            LineChart(context).apply {
//                description.isEnabled = false
//                setTouchEnabled(false)
//                isDragEnabled = false
//                setScaleEnabled(false)
//                xAxis.position = XAxis.XAxisPosition.BOTTOM
//                xAxis.setDrawGridLines(true)
//                xAxis.textColor = android.graphics.Color.GRAY
//                axisLeft.setDrawGridLines(true)
//                axisLeft.textColor = android.graphics.Color.GRAY
//                axisRight.isEnabled = false
//                legend.isEnabled = false
//
//                // ✅ Fixed Y-Axis Range
//                axisLeft.axisMinimum = 0f
//                axisLeft.axisMaximum = 20f
//            }
//        },
//        update = { chart ->
//            if (sensorData.isNotEmpty()) {
//                val minTime = sensorData.minOf { it.first }
//                val maxTime = sensorData.maxOf { it.first }
//
//                val entries = sensorData.map { (time, magnitude) -> Entry(time, magnitude) }
//
//                val dataSet = LineDataSet(entries, "Acceleration").apply {
//                    color = android.graphics.Color.BLUE
//                    valueTextColor = android.graphics.Color.BLACK
//                    setDrawCircles(false)
//                    setDrawValues(false)
//                    lineWidth = 2f
//                }
//
//                chart.data = LineData(dataSet)
//                chart.xAxis.axisMinimum = maxTime - 5f
//                chart.xAxis.axisMaximum = maxTime
//
//                chart.notifyDataSetChanged()
//                chart.invalidate()
//            }
//        },
//        modifier = modifier.fillMaxWidth(),
//
//        )
//}

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

                // ❌ Clean up X Axis
                xAxis.apply {
                    isEnabled = true  // keep enabled so data shows
                    setDrawGridLines(false)
                    setDrawLabels(false)
                    setDrawAxisLine(false)
                }

                // ❌ Clean up Left Axis
                axisLeft.apply {
                    isEnabled = true  // still needed for scaling
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
                    color = android.graphics.Color.rgb(0,100,0)
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

// Original FFT Graph

//@Composable
//fun FFTGraph(fftData: List<Pair<Float, Float>>, modifier: Modifier = Modifier) {
//    AndroidView(
//        factory = { context ->
//            LineChart(context).apply {
//                description.isEnabled = false
//                setTouchEnabled(false)
//                isDragEnabled = false
//                setScaleEnabled(false)
//                xAxis.position = XAxis.XAxisPosition.BOTTOM
//                xAxis.setDrawGridLines(true)
//                xAxis.textColor = android.graphics.Color.GRAY
//                xAxis.textSize = 12f
//                xAxis.labelRotationAngle = 0f
//                xAxis.valueFormatter = object : ValueFormatter() {
//                    override fun getFormattedValue(value: Float): String {
//                        return String.format("%.1f Hz", value)
//                    }
//                }
//                axisRight.isEnabled = false
//                legend.isEnabled = true
//            }
//        },
//        update = { chart ->
//            if (fftData.isNotEmpty()) {
//                val entries = fftData.map { (freq, power) -> Entry(freq, power) }
//
//                val dataSet = LineDataSet(entries, "FFT Power").apply {
//                    color = android.graphics.Color.BLUE
//                    valueTextColor = android.graphics.Color.BLACK
//                    setDrawCircles(false)
//                    setDrawValues(false)
//                    lineWidth = 2f
//                }
//
//                chart.data = LineData(dataSet)
//
//                val maxPower = fftData.maxOfOrNull { it.second } ?: 1000f
//                chart.axisLeft.axisMaximum = maxPower * 1.1f
//                chart.axisLeft.axisMinimum = 0f
//                chart.axisLeft.textColor = android.graphics.Color.GRAY
//
//                chart.notifyDataSetChanged()
//                chart.invalidate()
//            }
//        },
//        modifier = modifier.fillMaxWidth(),
//
//        )
//}


@Composable
fun AudioWaveformGraph(audioData: List<Short>, modifier: Modifier = Modifier) {
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
                        color = android.graphics.Color.BLUE
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

// original waveform graph:

//@Composable
//fun AudioWaveformGraph(audioData: List<Short>, modifier: Modifier = Modifier) {
//    AndroidView(
//        factory = { context ->
//            LineChart(context).apply {
//                description.text = "Audio Waveform"
//                setTouchEnabled(false)
//                setScaleEnabled(false)
//                xAxis.position = XAxis.XAxisPosition.BOTTOM
//                axisRight.isEnabled = false
//                axisLeft.axisMinimum = -32768f  // 16-bit audio range
//                axisLeft.axisMaximum = 32767f
//                legend.isEnabled = false
//            }
//        },
//        update = { chart ->
//            if (audioData.isNotEmpty()) {
//                val downsampled = audioData
//                    .withIndex()
//                    .filter { it.index % 100 == 0 }
//                    .mapNotNull {
//                        val y = it.value.toFloat()
//                        if (y.isFinite()) Entry(it.index.toFloat(), y) else null
//                    }
//
//                if (downsampled.isNotEmpty()) {
//                    val dataSet = LineDataSet(downsampled, "Audio").apply {
//                        color = android.graphics.Color.BLUE
//                        valueTextColor = android.graphics.Color.BLACK
//                        setDrawCircles(false)
//                        setDrawValues(false)
//                        lineWidth = 1f
//                    }
//
//                    try {
//                        chart.data = LineData(dataSet)
//                        chart.notifyDataSetChanged()
//                        chart.invalidate()
//                    } catch (e: Exception) {
//                        Log.e("ChartError", "Chart update failed: ${e.localizedMessage}")
//                    }
//                }
//            }
//        },
//        modifier = modifier
//            .fillMaxWidth()
//            .height(200.dp)
//    )
//}


@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            BottomAppBar(
                containerColor = androidx.compose.ui.graphics.Color.LightGray
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = {
                    // Exit the app
                    (context as? Activity)?.finish()
                }) {
                    Text("Exit")
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFFB0B0B0))
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
                text = "GloveWorks",
                style = MaterialTheme.typography.headlineLarge,
                color = androidx.compose.ui.graphics.Color.LightGray
            )

            Spacer(modifier = Modifier.height(36.dp))


            Button(
                onClick = { navController.navigate("main") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("Gait Monitor")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { navController.navigate("voice") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("Voice Analysis")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { navController.navigate("glove_control") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("Glove Control")
            }

            Spacer(modifier = Modifier.height(160.dp))

            Button(
                onClick = { navController.navigate("settings") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("Settings")
            }

        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(navController: NavHostController) {
    val context = LocalContext.current
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

            delay(500) // Give it time to update
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

//    println("Buffer size = $bufferSize")


    var audioRecord by remember { mutableStateOf<AudioRecord?>(null) }
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
        val sharedPref = context.getSharedPreferences("gloveworks_prefs",
            Context.MODE_PRIVATE)
        sharedPref.edit().remove("voice_session_id").apply()

        collectedSamples = mutableListOf()
        rmsValues = mutableListOf()
        epochFreqList = mutableListOf()
        fftResults = emptyList()

        coroutineScope.launch {
            withContext(Dispatchers.Main) {
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

                    //  Step 1: Run RMS and FFT on IO thread
                    val (rms, fftData, dominantFreq) = withContext(Dispatchers.Default) {
                        val rms = calculateRMS(epochSamples)
                        val fftData = computeFFT(epochSamples, sampleRate)
                        val dominantFreq = fftData.maxByOrNull { it.second }?.first ?: 0f
                        Triple(rms, fftData, dominantFreq)
                    }

                    //  Step 2: Upload and update UI on Main thread
                    withContext(Dispatchers.Main) {
                        collectedSamples.addAll(epochSamples)
                        rmsValues.add(rms)
                        epochFreqList.add(Pair(epochIndex + 1, dominantFreq))
                    }

                    uploadRawAudioToStorage(
                        context = context,
                        epochIndex = epochIndex + 1,
                        audioData = epochSamples
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
                }
            }
        }
    }



    fun stopRecording() {
        coroutineScope.launch {
//            println("Stop button clicked")
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
//                println("StopRecording error: ${e.message}")
            }
            audioRecord = null
        }
    }


    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Voice Analysis") }) },
        bottomBar = {
            BottomAppBar {
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { navController.navigate("home") }) {
                    Text("Home")
                }
                Spacer(modifier = Modifier.weight(1f))
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
                                .background(Color.Red)
                        )
                    }

                }
            }

            Button(
                onClick = {
                    if (permissionState.value && !isRecording) {
                        startRecording()
                    } else {
                        println("Microphone permission not granted.")
                    }
                },
                enabled = !isRecording,
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
            RMSGraph(rmsValues, modifier=Modifier.height(150.dp))
            FFTAudioTrendGraph(epochFreqList, modifier=Modifier.height(150.dp))


        }
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

                // ❌ Clean up X Axis
                xAxis.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    setDrawLabels(false)
                    setDrawAxisLine(false)
                }

                // ❌ Clean up Left Axis
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
                    color = android.graphics.Color.rgb(0,100,0)
                    setDrawCircles(false)
                    circleRadius = 4f
                    setDrawValues(false) // ❌ Removed value labels
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

//Original RMSGraph

//@Composable
//fun RMSGraph(rmsValues: List<Float>, modifier: Modifier = Modifier) {
//    AndroidView(
//        factory = { context ->
//            LineChart(context).apply {
//                description.text = "RMS Over Time"
//                setTouchEnabled(false)
//                setScaleEnabled(false)
//                setPinchZoom(false)
//                isDragEnabled = false
//
//                xAxis.position = XAxis.XAxisPosition.BOTTOM
//                xAxis.setDrawGridLines(true)
//                xAxis.textColor = android.graphics.Color.GRAY
//                xAxis.textSize = 12f
//                xAxis.labelRotationAngle = 0f
//                xAxis.valueFormatter = object : ValueFormatter() {
//                    override fun getFormattedValue(value: Float): String {
//                        return "${(value.toInt() + 1) * 5}s"
//                    }
//                }
//
//                axisLeft.axisMinimum = 0f
//                axisLeft.textColor = android.graphics.Color.GRAY
//                axisLeft.valueFormatter = object : ValueFormatter() {
//                    override fun getFormattedValue(value: Float): String {
//                        return String.format("%.0f", value)
//                    }
//                }
//
//                axisRight.isEnabled = false
//                legend.isEnabled = false
//            }
//        },
//        update = { chart ->
//            if (rmsValues.isNotEmpty()) {
//                val entries = rmsValues.mapIndexed { index, value ->
//                    Entry(index.toFloat(), value)
//                }
//
//                val dataSet = LineDataSet(entries, "RMS").apply {
//                    color = android.graphics.Color.BLUE
//                    setDrawCircles(true)
//                    circleRadius = 4f
//                    valueTextColor = android.graphics.Color.BLACK
//                    setDrawValues(true)
//                    lineWidth = 2f
//                }
//
//                chart.data = LineData(dataSet)
//                chart.notifyDataSetChanged()
//                chart.invalidate()
//            }
//        },
//        modifier = modifier
//            .fillMaxWidth()
//            .height(200.dp)
//    )
//}

@Composable
fun FFTAudioTrendGraph(epochFreqList: List<Pair<Int, Float>>, modifier: Modifier = Modifier) {
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
                    axisMaximum = 40f  // initial cap; will be overridden below
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
                    color = android.graphics.Color.MAGENTA
                    setDrawCircles(false)
                    circleRadius = 4f
                    setDrawValues(false)  // ❌ removes text above points
                    lineWidth = 2f
                    mode = LineDataSet.Mode.LINEAR  // ✅ smooth connection
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

//original audio fft trend graph:

//@Composable
//fun FFTAudioTrendGraph(epochFreqList: List<Pair<Int, Float>>, modifier: Modifier = Modifier) {
//    AndroidView(
//        factory = { context ->
//            LineChart(context).apply {
//                description.text = "Dominant Frequency Over Time"
//                setTouchEnabled(false)
//                setScaleEnabled(false)
//                setPinchZoom(false)
//                isDragEnabled = false
//
//                xAxis.position = XAxis.XAxisPosition.BOTTOM
//                xAxis.setDrawGridLines(true)
//                xAxis.textColor = android.graphics.Color.GRAY
//                xAxis.textSize = 12f
//                xAxis.labelRotationAngle = 0f
//                xAxis.valueFormatter = object : ValueFormatter() {
//                    override fun getFormattedValue(value: Float): String {
//                        return "${value.toInt() * 5}s"
//                    }
//                }
//
//                axisLeft.axisMinimum = 0f
//                axisLeft.axisMaximum = 12f
//                axisLeft.textColor = android.graphics.Color.GRAY
//                axisLeft.valueFormatter = object : ValueFormatter() {
//                    override fun getFormattedValue(value: Float): String {
//                        return String.format("%.1f Hz", value)
//                    }
//                }
//
//                axisRight.isEnabled = false
//                legend.isEnabled = false
//            }
//        },
//        update = { chart ->
//            if (epochFreqList.isNotEmpty()) {
//                val maxY = epochFreqList.maxOfOrNull { it.second }?.coerceAtLeast(20f) ?: 20f
//                chart.axisLeft.axisMaximum = maxY
//
//                val entries = epochFreqList.map { Entry(it.first.toFloat(), it.second) }
//                val dataSet = LineDataSet(entries, "Dominant Frequency").apply {
//                    color = android.graphics.Color.MAGENTA
//                    setDrawCircles(true)
//                    circleRadius = 4f
//                    valueTextColor = android.graphics.Color.BLACK
//                    setDrawValues(true)
//                    lineWidth = 2f
//                }
//                chart.data = LineData(dataSet)
//                chart.notifyDataSetChanged()
//                chart.invalidate()
//            }
//        },
//        modifier = modifier.fillMaxWidth().height(200.dp)
//    )
//}

@Composable
fun FFTTrendGraph(epochFreqList: List<Pair<Int, Float>>, modifier: Modifier = Modifier) {
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
                    isEnabled = true  // Needed to draw correctly
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
                    color = android.graphics.Color.MAGENTA
                    setDrawCircles(false)
                    circleRadius = 4f
                    setDrawValues(false)  // ❌ Removes the value text if you want
                    lineWidth = 3f
                    mode = LineDataSet.Mode.LINEAR  // ✅ Smooth connection
                }

                chart.data = LineData(dataSet)
                chart.notifyDataSetChanged()
                chart.invalidate()
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

// original FFTTrendGraph

//@Composable
//fun FFTTrendGraph(epochFreqList: List<Pair<Int, Float>>, modifier: Modifier = Modifier) {
//    AndroidView(
//        factory = { context ->
//            LineChart(context).apply {
//                description.text = "Dominant Frequency Over Time"
//                setTouchEnabled(false)
//                setScaleEnabled(false)
//                setPinchZoom(false)
//                isDragEnabled = false
//
//                xAxis.position = XAxis.XAxisPosition.BOTTOM
//                xAxis.setDrawGridLines(true)
//                xAxis.textColor = android.graphics.Color.GRAY
//                xAxis.textSize = 12f
//                xAxis.labelRotationAngle = 0f
//                xAxis.valueFormatter = object : ValueFormatter() {
//                    override fun getFormattedValue(value: Float): String {
//                        return "${value.toInt() * 5}s"
//                    }
//                }
//
//                axisLeft.axisMinimum = 0f
//                axisLeft.axisMaximum = 12f
//                axisLeft.textColor = android.graphics.Color.GRAY
//                axisLeft.valueFormatter = object : ValueFormatter() {
//                    override fun getFormattedValue(value: Float): String {
//                        return String.format("%.1f Hz", value)
//                    }
//                }
//
//                axisRight.isEnabled = false
//                legend.isEnabled = false
//            }
//        },
//        update = { chart ->
//            if (epochFreqList.isNotEmpty()) {
//                val entries = epochFreqList.map { Entry(it.first.toFloat(), it.second) }
//                val dataSet = LineDataSet(entries, "Dominant Frequency").apply {
//                    color = android.graphics.Color.MAGENTA
//                    setDrawCircles(true)
//                    circleRadius = 4f
//                    valueTextColor = android.graphics.Color.BLACK
//                    setDrawValues(true)
//                    lineWidth = 2f
//                }
//                chart.data = LineData(dataSet)
//                chart.notifyDataSetChanged()
//                chart.invalidate()
//            }
//        },
//        modifier = modifier.fillMaxWidth(),
//
//
//    )
//}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController, userInfoViewModel: UserInfoViewModel) {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)


    var isCollecting by remember {
        mutableStateOf(false) }
    var sensorDataList by remember {
        mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var fftDataList by remember {
        mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var epochFreqList by remember {
        mutableStateOf<List<Pair<Int, Float>>>(emptyList()) }
    var elapsedTime by remember {
        mutableStateOf(0f) }
    var epochSamples by remember {
        mutableStateOf(mutableListOf<Triple<Float, Float, Float>>()) }


    val coroutineScope = rememberCoroutineScope()

    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (isCollecting) {
                        val x = it.values[0]
                        val y = it.values[1]
                        val z = it.values[2]
                        val magnitude = sqrt(x * x + y * y + z * z)
                        elapsedTime += 0.1f
                        sensorDataList = sensorDataList + Pair(elapsedTime, magnitude)
                        sensorDataList = sensorDataList.filter { it.first >= elapsedTime - 5f }
                        epochSamples.add(Triple(x, y, z))
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
        val sharedPref = context.getSharedPreferences("gloveworks_prefs",
            Context.MODE_PRIVATE)
        sharedPref.edit().remove("motion_session_id").apply() // Consistent key

        isCollecting = true
        sensorDataList = emptyList()
        fftDataList = emptyList()
        epochFreqList = emptyList()
        elapsedTime = 0f

        sensorManager.registerListener(sensorListener, accelerometer,
            SensorManager.SENSOR_DELAY_UI)

        coroutineScope.launch {
            for (epochIndex in 1..4) {
                epochSamples = mutableListOf()

                val cycleStartTime = System.currentTimeMillis()
                while (isCollecting) {
                    val elapsedMillis = System.currentTimeMillis() - cycleStartTime
                    if (elapsedMillis >= 5000) break
                    delay(100)
                }
                if (!isCollecting) break

                val accelerationValues = sensorDataList.map { it.second }
                val fftResult = computeFFT(accelerationValues)
                fftDataList = fftResult

                val dominantFrequency = fftResult.maxByOrNull { it.second }?.first ?: 0f
                epochFreqList = epochFreqList + Pair(epochIndex, dominantFrequency)

                uploadMotionEpochToFirebase(
                    context = context,
                    epochIndex = epochIndex, // ✅ fixed
                    accelerometerSamples = epochSamples
                )
            }

            stopCollecting()
        }
    }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("GloveWorks Gait Monitor") })
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = androidx.compose.ui.graphics.Color.LightGray
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { navController.navigate("home") }) {
                    Text("Home")
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFFB0B0B0))
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
                    enabled = !isCollecting,
                    modifier = Modifier.weight(1f).height(60.dp)
                ) { Text("Start") }

                Button(
                    onClick = { stopCollecting() },
                    enabled = isCollecting,
                    modifier = Modifier.weight(1f).height(60.dp)
                ) { Text("Stop") }
            }

            Spacer(modifier = Modifier.height(16.dp))



            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp) // You can adjust this as needed
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RealTimeGraph(sensorDataList, modifier = Modifier.weight(1f))
                FFTGraph(fftDataList, modifier = Modifier.weight(1f))
                FFTTrendGraph(epochFreqList, modifier = Modifier.weight(1f))
            }



        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun SettingsScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") }
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = androidx.compose.ui.graphics.Color.LightGray
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { navController.navigate("home") }) {
                    Text("Home")
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }

    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(androidx.compose.ui.graphics.Color(0xFFB0B0B0)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
                ) {

                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { navController.navigate("user_info") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)) {
                    Text("User Information")

                    Spacer(modifier = Modifier.height(10.dp))}

                Spacer(modifier = Modifier.height(200.dp))

                Button(
                    onClick = { navController.navigate("about") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)

                    ) {
                        Text("About")
                    }
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
                     Text("for Española GloveWorks")
                     Text("at Northern New Mexico College, 2025", style = MaterialTheme.typography.bodyMedium)


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

        // Bottom Back Button
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .padding(bottom = 24.dp)
        ) {
            Text("Back")
        }
    }
}


@Composable
fun GloveControlScreen(navController: NavHostController) {
    Scaffold(
        bottomBar = {
            BottomAppBar {
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Home")
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Coming Soon!", style = MaterialTheme.typography.headlineMedium)
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(navController: NavHostController) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("gloveworks_prefs",
        Context.MODE_PRIVATE)

    val userId = sharedPref.getString("user_id", null)
    val userType =
        if (userId?.startsWith("glw_anon_") == true) "Anonymous" else "GloveWorks Participant"

    val database = FirebaseDatabase.getInstance().reference
    var userInfo by remember { mutableStateOf<Map<String, Any>?>(null) }

    // Load user data from Firebase
    LaunchedEffect(userId) {
        userId?.let {
            database.child("users").child(it).get().addOnSuccessListener {
                snapshot ->
                userInfo = snapshot.value as? Map<String, Any>
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("User Information") }
            )
        },
        bottomBar = {
            BottomAppBar {
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Back")
                }
                Spacer(modifier = Modifier.weight(1f))
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

            if (userId == null) {
                Text("No user found.")
                return@Column
            }

            Text("User Type: $userType")
            Text("User ID: $userId")

            Spacer(modifier = Modifier.height(16.dp))

            userInfo?.let { info ->
                val profile = info["profile"] as? Map<*, *>
                when (userType) {
                    "GloveWorks Participant" -> {
                        Text("Name: ${profile?.get("name") ?: "N/A"} ${profile?.get("lastName") ?: ""}")
                        Text("Email: ${profile?.get("email") ?: "N/A"}")
                        Text("Date of Birth: ${profile?.get("dob") ?: "N/A"}")
                        Text("Phone: ${profile?.get("phone") ?: "N/A"}")

                        val pdqAnswers = info["pdq"] as? List<*>
                        if (pdqAnswers != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("PDQ-8 Questionnaire: Submitted")
                        }
                    }

                    "Anonymous" -> {
                        Text("Age: ${profile?.get("age") ?: "N/A"}")
                        Text("Diagnosed with PD: ${profile?.get("diagnosedWithPD") ?: "N/A"}")

                        val consent = info["consent"] as? Map<*, *>
                        if (consent != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Sharing Motion: ${consent["motion"]}")
                            Text("Sharing Voice: ${consent["voice"]}")
                        }
                    }
                }
            } ?: run {
                Text("Loading user data...")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                sharedPref.edit()
                    .clear()
                    .remove("motion_session_id")
                    .remove("voice_session_id")
                    .apply()
                context.startActivity(intent)
            }) {
                Text("Reset and Re-Onboard")
            }
        }
    }
}









