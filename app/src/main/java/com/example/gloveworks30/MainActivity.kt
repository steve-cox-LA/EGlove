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
import androidx.compose.ui.unit.sp
import com.google.firebase.database.FirebaseDatabase
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)


        setContent {
            Gloveworks30Theme {
                val navController = rememberNavController()
                val userInfoViewModel: UserInfoViewModel = viewModel()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") { HomeScreen(navController) }
                    composable("main") { MainScreen(navController, userInfoViewModel) }
                    composable("voice") { VoiceScreen(navController) }
                    composable("settings") { SettingsScreen(navController) }
                    composable("user_info") { UserInfoScreen(navController, userInfoViewModel) }


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
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(true)
                xAxis.textColor = android.graphics.Color.GRAY // ✅ Gray X-axis labels
                axisLeft.setDrawGridLines(true)
                axisLeft.textColor = android.graphics.Color.GRAY // ✅ Gray Y-axis labels
                axisRight.isEnabled = false
                legend.isEnabled = false

                // ✅ Fixed Y-Axis Range
                axisLeft.axisMinimum = 0f
                axisLeft.axisMaximum = 20f
            }
        },
        update = { chart ->
            if (sensorData.isNotEmpty()) {
                val minTime = sensorData.minOf { it.first }
                val maxTime = sensorData.maxOf { it.first }

                val entries = sensorData.map { (time, magnitude) -> Entry(time, magnitude) }

                val dataSet = LineDataSet(entries, "Acceleration").apply {
                    color = android.graphics.Color.BLUE
                    valueTextColor = android.graphics.Color.BLACK
                    setDrawCircles(false)
                    setDrawValues(false)
                    lineWidth = 2f
                }

                chart.data = LineData(dataSet)
                chart.xAxis.axisMinimum = maxTime - 5f
                chart.xAxis.axisMaximum = maxTime

                chart.notifyDataSetChanged()
                chart.invalidate()
            }
        },
        modifier = modifier.fillMaxWidth(), // ✅ Don't override height!

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

fun uploadEpochToFirebase(
    epochIndex: Int,
    sensorData: List<Pair<Float, Float>>,
    dominantFrequency: Float,
    firstName: String?,
    lastName: String?
) {
    val db = FirebaseDatabase.getInstance()
    val ref = db.getReference("sessions").push()

    val timestamp = System.currentTimeMillis()

    val dataMap = mapOf(
        "timestamp" to timestamp,
        "epoch" to epochIndex,
        "dominantFrequency" to dominantFrequency,
        "firstName" to (firstName ?: "N/A"),
        "lastName" to (lastName ?: "N/A"),
        "accelerometerData" to sensorData.map { mapOf("time" to it.first, "magnitude" to it.second) }
    )

    ref.setValue(dataMap)
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
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(true)
                xAxis.textColor = android.graphics.Color.GRAY
                xAxis.textSize = 12f
                xAxis.labelRotationAngle = 0f
                xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return String.format("%.1f Hz", value)
                    }
                }
                axisRight.isEnabled = false
                legend.isEnabled = true
            }
        },
        update = { chart ->
            if (fftData.isNotEmpty()) {
                val entries = fftData.map { (freq, power) -> Entry(freq, power) }

                val dataSet = LineDataSet(entries, "FFT Power").apply {
                    color = android.graphics.Color.BLUE
                    valueTextColor = android.graphics.Color.BLACK
                    setDrawCircles(false)
                    setDrawValues(false)
                    lineWidth = 2f
                }

                chart.data = LineData(dataSet)

                val maxPower = fftData.maxOfOrNull { it.second } ?: 1000f
                chart.axisLeft.axisMaximum = maxPower * 1.1f
                chart.axisLeft.axisMinimum = 0f
                chart.axisLeft.textColor = android.graphics.Color.GRAY

                chart.notifyDataSetChanged()
                chart.invalidate()
            }
        },
        modifier = modifier.fillMaxWidth(), // ✅ Don't override height!


    )
}

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun HomeScreen(navController: NavHostController) {
    Scaffold(
        bottomBar = {
            BottomAppBar(
                containerColor = androidx.compose.ui.graphics.Color.LightGray
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { navController.navigate("settings") }) {
                    Text("Settings")
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Voice Analysis") })
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
            Text(
                text = "Coming Soon!",
                style = MaterialTheme.typography.headlineMedium,
                color = androidx.compose.ui.graphics.Color.Black
            )
        }
    }
}

@Composable
fun FFTTrendGraph(epochFreqList: List<Pair<Int, Float>>, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.text = "Dominant Frequency Over Time"
                setTouchEnabled(false)
                setScaleEnabled(false)
                setPinchZoom(false)
                isDragEnabled = false

                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(true)
                xAxis.textColor = android.graphics.Color.GRAY
                xAxis.textSize = 12f
                xAxis.labelRotationAngle = 0f
                xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "${value.toInt() * 5}s"
                    }
                }

                axisLeft.axisMinimum = 0f
                axisLeft.axisMaximum = 12f
                axisLeft.textColor = android.graphics.Color.GRAY
                axisLeft.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return String.format("%.1f Hz", value)
                    }
                }

                axisRight.isEnabled = false
                legend.isEnabled = false
            }
        },
        update = { chart ->
            if (epochFreqList.isNotEmpty()) {
                val entries = epochFreqList.map { Entry(it.first.toFloat(), it.second) }
                val dataSet = LineDataSet(entries, "Dominant Frequency").apply {
                    color = android.graphics.Color.MAGENTA
                    setDrawCircles(true)
                    circleRadius = 4f
                    valueTextColor = android.graphics.Color.BLACK
                    setDrawValues(true)
                    lineWidth = 2f
                }
                chart.data = LineData(dataSet)
                chart.notifyDataSetChanged()
                chart.invalidate()
            }
        },
        modifier = modifier.fillMaxWidth(), // ✅ Don't override height!


    )
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController, userInfoViewModel: UserInfoViewModel) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val firstName = userInfoViewModel.firstName
    val lastName = userInfoViewModel.lastName


    var isCollecting by remember { mutableStateOf(false) }
    var sensorDataList by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var fftDataList by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var epochFreqList by remember { mutableStateOf<List<Pair<Int, Float>>>(emptyList()) }
    var elapsedTime by remember { mutableStateOf(0f) }

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
        isCollecting = true
        sensorDataList = emptyList()
        fftDataList = emptyList()
        epochFreqList = emptyList()
        elapsedTime = 0f

        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)

        coroutineScope.launch {
            for (epochIndex in 1..4) {
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

                // ✅ Upload to Firebase
                uploadEpochToFirebase(
                    epochIndex,
                    sensorDataList,
                    dominantFrequency,
                    firstName,
                    lastName
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


//            val graphHeight = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 16.dp)
//                .weight(1f)
//
//            val totalHeight = Modifier
//                .fillMaxWidth()
//                .fillMaxHeight()

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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Settings", color = androidx.compose.ui.graphics.Color.Black, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { navController.navigate("user_info") }) {
                    Text("User Information")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(navController: NavHostController, userInfoViewModel: UserInfoViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("User Information") })
        },
        snackbarHost = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .padding(16.dp)
                        .wrapContentSize()
                )
            }
        },

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = userInfoViewModel.firstName,
                onValueChange = { userInfoViewModel.updateFirstName(it) },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = userInfoViewModel.lastName,
                onValueChange = { userInfoViewModel.updateLastName(it) },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Saved!", duration = SnackbarDuration.Short)
                    }
                }, modifier = Modifier.weight(1f)) {
                    Text("Save")
                }

                Button(onClick = {
                    userInfoViewModel.resetUserInfo()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Reset!", duration = SnackbarDuration.Short)
                    }
                }, modifier = Modifier.weight(1f)) {
                    Text("Reset")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(onClick = { navController.navigateUp() }, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
        }
    }
}

