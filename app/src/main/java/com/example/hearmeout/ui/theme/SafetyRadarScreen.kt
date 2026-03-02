package com.example.hearmeout

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.hearmeout.model.DetectedSound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * SafetyRadarScreen: Monitors the acoustic environment in real-time using AI (YAMNet).
 * Provides multi-sensory alerts (Visual Flash, Haptic Vibration, UI Radar) for danger sounds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyRadarScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val detectedSounds = remember { mutableStateListOf<DetectedSound>() }
    val scope = rememberCoroutineScope()

    // Permissions handling - Ensuring the app doesn't crash if permissions are denied
    var hasAudioPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
    }

    /**
     * Camera Manager setup for Visual Alert (Flashlight).
     * Includes safety checks for device compatibility.
     */
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val cameraId = remember {
        try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) { null }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (!hasAudioPermission) permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        if (!hasCameraPermission) permissionsToRequest.add(Manifest.permission.CAMERA)
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    /**
     * Estimates sound source distance based on decibel levels using an
     * approximation of the Inverse Square Law.
     */
    fun estimateDistance(decibels: Double): Double {
        val calculatedDistance = (100.0 - decibels) * 0.3
        return max(0.5, calculatedDistance)
    }

    /**
     * Triggers hardware alerts (Haptic Feedback + Flashlight) when danger is detected.
     * Implemented using background coroutines to prevent UI blocking (ANR prevention).
     */
    fun triggerDangerAlerts() {
        // Haptic Feedback implementation with SDK version checks
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                val pattern = longArrayOf(0, 300, 150, 300)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val amplitudes = intArrayOf(0, 255, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        // Flashlight strobe effect implementation
        scope.launch(Dispatchers.IO) {
            if (cameraId != null && hasCameraPermission) {
                try {
                    for (i in 0..3) {
                        cameraManager.setTorchMode(cameraId, true)
                        delay(250)
                        cameraManager.setTorchMode(cameraId, false)
                        delay(250)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    /**
     * Audio Classification Lifecycle:
     * Initializes the TFLite model, starts the AudioRecord buffer,
     * and performs continuous inference on a background thread.
     */
    DisposableEffect(hasAudioPermission) {
        var classifier: AudioClassifier? = null
        var record: android.media.AudioRecord? = null

        if (hasAudioPermission) {
            try {
                // Initialize YAMNet AI Model for sound recognition
                classifier = AudioClassifier.createFromFile(context, "yamnet.tflite")
                val audioTensor = classifier.createInputTensorAudio()
                record = classifier.createAudioRecord()
                record.startRecording()

                scope.launch(Dispatchers.IO) {
                    while (isActive) {
                        audioTensor.load(record)

                        // Calculate Root Mean Square (RMS) to determine Decibel (dB) level dynamically
                        val audioData = audioTensor.tensorBuffer.floatArray
                        var sumSquare = 0.0
                        for (sample in audioData) { sumSquare += (sample * sample).toDouble() }
                        val amplitude = sqrt(sumSquare / audioData.size)
                        val currentDecibels = if (amplitude > 0) max(0.0, 20 * log10(amplitude) + 90.0) else 0.0

                        val estimatedDistance = estimateDistance(currentDecibels)
                        val results = classifier.classify(audioTensor)

                        if (results.isNotEmpty() && results[0].categories.isNotEmpty()) {
                            val topCategory = results[0].categories[0]

                            // Filtering out low-confidence results and silence
                            if (topCategory.score > 0.3f && topCategory.label != "Silence" && topCategory.label != "Background noise") {
                                val label = topCategory.label

                                // Heuristic danger detection based on label keywords and dB levels
                                val isDangerLabel = label.contains("Alarm", ignoreCase = true) ||
                                        label.contains("Siren", ignoreCase = true) ||
                                        label.contains("Emergency", ignoreCase = true) ||
                                        label.contains("Glass", ignoreCase = true) ||
                                        label.contains("Cry", ignoreCase = true)

                                val isVeryLoud = currentDecibels > 80.0
                                val isDanger = isDangerLabel || isVeryLoud

                                launch(Dispatchers.Main) {
                                    val lastSound = detectedSounds.firstOrNull()
                                    // Debounce filter to prevent flooding the UI with the same sound event
                                    if (lastSound == null || lastSound.name != label || (System.currentTimeMillis() - lastSound.timestamp > 3000)) {
                                        detectedSounds.add(0, DetectedSound(label, isDanger, System.currentTimeMillis(), currentDecibels, estimatedDistance))
                                        if (isDanger) {
                                            triggerDangerAlerts()
                                        }
                                    }
                                }
                            }
                        }
                        delay(500) // Inference frequency (2Hz)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * Resource Management: Ensuring AudioRecord and Classifier are released
         * to prevent memory leaks or hardware locks.
         */
        onDispose {
            try {
                record?.stop()
                record?.release()
                classifier?.close()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safety Radar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF001F3F)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            RadarAnimation(isActive = hasAudioPermission)
            Spacer(modifier = Modifier.height(32.dp))

            if (!hasAudioPermission) {
                Text("Waiting for permissions...", color = Color.Red, fontWeight = FontWeight.Bold)
            } else {
                Text("Listening continuously...", color = Color(0xFF40E0D0), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            ContainerCard(modifier = Modifier.weight(1f)) {
                if (detectedSounds.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Waiting for sounds...", color = Color.Gray, fontSize = 16.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(detectedSounds) { sound ->
                            DetectedSoundItem(sound)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Visual Volume Indicator: Maps decibel levels to a 5-step discrete UI element.
 */
@Composable
fun VolumeCircles(decibels: Double) {
    val level = when {
        decibels < 40 -> 1
        decibels < 60 -> 2
        decibels < 75 -> 3
        decibels < 85 -> 4
        else -> 5
    }

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..5) {
            val color = if (i <= level) {
                when (i) {
                    1, 2 -> Color(0xFF4CAF50)
                    3 -> Color(0xFFFFEB3B)
                    4 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
            } else {
                Color(0xFFE0E0E0)
            }
            Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        }
    }
}

/**
 * Radar Animation: Infinite transition simulating an active sonar scan.
 */
@Composable
fun RadarAnimation(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "alpha"
    )

    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        if (isActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2
                drawCircle(color = Color(0xFF40E0D0).copy(alpha = alpha), radius = radius * scale, style = Stroke(width = 4.dp.toPx()))
                drawCircle(color = Color(0xFF40E0D0).copy(alpha = alpha * 0.5f), radius = radius * (scale * 0.5f), style = Stroke(width = 2.dp.toPx()))
            }
        }
        Box(
            modifier = Modifier.size(60.dp).background(if (isActive) Color(0xFF40E0D0) else Color.DarkGray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = "Radar Status", tint = Color(0xFF001F3F), modifier = Modifier.size(32.dp))
        }
    }
}

/**
 * DetectedSoundItem: UI representation of an inferred sound event.
 * Uses high-contrast styling for dangerous events.
 */
@Composable
fun DetectedSoundItem(sound: DetectedSound) {
    val date = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(sound.timestamp))
    val decibelsString = String.format("%.1f dB", sound.decibels)
    val distanceString = String.format("~%.1f m", sound.distance)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = if (sound.isDangerous) Color(0xFFFFEbee) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = sound.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (sound.isDangerous) Color.Red else Color.Black)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VolumeCircles(decibels = sound.decibels)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "$decibelsString | $distanceString | $date", fontSize = 12.sp, color = Color.Gray)
                }
            }
            if (sound.isDangerous) {
                Icon(Icons.Default.NotificationsActive, contentDescription = "Danger Warning", tint = Color.Red)
            }
        }
    }
}

@Composable
fun ContainerCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier.fillMaxWidth().background(color = Color(0xFFF8F9FA), shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).padding(top = 16.dp)
    ) { content() }
}