package com.example.hearmeout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Data model representing a location with acoustic and busyness data.
 * Designed for Firebase Firestore integration.
 */
data class PlaceData(
    val id: String = "",
    val nameHe: String = "",
    val nameEn: String = "",
    val typeHe: String = "",
    val typeEn: String = "",
    val busynessPercentage: Int = 0,
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

/**
 * AccessibilityMapScreen: Displays a Google Map with real-time noise level data
 * synchronized with Firebase Firestore for community-driven updates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilityMapScreen(onBack: () -> Unit) {
    var isHebrew by remember { mutableStateOf(true) }
    val layoutDirection = if (isHebrew) LayoutDirection.Rtl else LayoutDirection.Ltr

    // State for storing places fetched from the cloud database
    var places by remember { mutableStateOf<List<PlaceData>>(emptyList()) }
    var selectedPlaceToReport by remember { mutableStateOf<PlaceData?>(null) }

    val db = FirebaseFirestore.getInstance()

    // Real-time synchronization with Firestore collection "places"
    LaunchedEffect(Unit) {
        val placesRef = db.collection("places")
        placesRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null && !snapshot.isEmpty) {
                // Map Firestore documents to PlaceData objects
                val loadedPlaces = snapshot.documents.mapNotNull { it.toObject(PlaceData::class.java) }
                places = loadedPlaces
            } else {
                // Initialize database with default values if empty on first run
                val initialPlaces = listOf(
                    PlaceData("1", "קפה ארומה", "Café Aroma", "בית קפה", "Coffee Shop", 25, 32.0735, 34.7780),
                    PlaceData("2", "בורגר קינג", "Burger King", "מזון מהיר", "Fast Food", 85, 32.0745, 34.7770),
                    PlaceData("3", "הספרייה העירונית", "Central Library", "ציבורי", "Public", 10, 32.0785, 34.7820),
                    PlaceData("4", "סושי בר", "Sushi Bar", "מסעדה", "Restaurant", 55, 32.0700, 34.7750),
                    PlaceData("5", "פאב אירי", "Irish Pub", "בר", "Bar", 95, 32.0800, 34.7800)
                )
                initialPlaces.forEach { place ->
                    placesRef.document(place.id).set(place)
                }
            }
        }
    }

    val title = if (isHebrew) "מפת נגישות" else "Accessibility Map"
    val subtitle = if (isHebrew) "עומס ורעש בזמן אמת (מדווח ע\"י הקהילה)" else "Live Crowd-sourced Acoustic Data"
    val startLocation = LatLng(32.0750, 34.7785)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startLocation, 14f)
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(title, fontWeight = FontWeight.Bold)
                            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Button(
                            onClick = { isHebrew = !isHebrew },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF40E0D0)),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = "Translate", tint = Color(0xFF001F3F), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHebrew) "EN" else "עב", color = Color(0xFF001F3F), fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
                    .padding(padding)
            ) {
                // Upper half: Google Maps UI
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    cameraPositionState = cameraPositionState
                ) {
                    places.forEach { place ->
                        val location = LatLng(place.lat, place.lng)
                        val decibels = calculateDecibels(place.busynessPercentage)
                        val statusColor = when {
                            decibels < 65 -> Color(0xFF4CAF50)
                            decibels < 80 -> Color(0xFFFFB300)
                            else -> Color(0xFFD32F2F)
                        }

                        val displayName = if (isHebrew) place.nameHe else place.nameEn
                        val displayBusyness = if (isHebrew) "עומס: ${place.busynessPercentage}%" else "Busyness: ${place.busynessPercentage}%"

                        MarkerInfoWindow(
                            state = MarkerState(position = location),
                            content = {
                                Column(
                                    modifier = Modifier
                                        .background(Color.White, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = displayName, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "${decibels} dB", fontWeight = FontWeight.Bold, color = statusColor, fontSize = 22.sp)
                                    Text(text = displayBusyness, color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        )
                    }
                }

                // Lower half: Nearby Places List
                Text(
                    text = if (isHebrew) "מקומות בסביבה" else "Nearby Places",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF001F3F),
                    modifier = Modifier.padding(16.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(places) { place ->
                        PlaceNoiseCard(
                            place = place,
                            isHebrew = isHebrew,
                            onReportClick = { selectedPlaceToReport = place }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        // Dialog for user-submitted noise reports (Crowdsourcing feature)
        if (selectedPlaceToReport != null) {
            ReportNoiseDialog(
                place = selectedPlaceToReport!!,
                isHebrew = isHebrew,
                onDismiss = { selectedPlaceToReport = null },
                onSubmit = { newBusyness ->
                    // Update server data in real-time
                    db.collection("places")
                        .document(selectedPlaceToReport!!.id)
                        .update("busynessPercentage", newBusyness)
                    selectedPlaceToReport = null
                }
            )
        }
    }
}

/**
 * Heuristic logic to estimate decibel levels based on crowd busyness percentage.
 */
fun calculateDecibels(busyness: Int): Int {
    return when {
        busyness <= 20 -> 50 + (busyness / 2)
        busyness <= 60 -> 60 + ((busyness - 20) / 2)
        else -> 80 + ((busyness - 60) / 2)
    }
}

@Composable
fun PlaceNoiseCard(place: PlaceData, isHebrew: Boolean, onReportClick: () -> Unit) {
    val decibels = calculateDecibels(place.busynessPercentage)

    val (statusColor, statusText) = when {
        decibels < 65 -> Color(0xFF4CAF50) to if (isHebrew) "שקט" else "Quiet"
        decibels < 80 -> Color(0xFFFFB300) to if (isHebrew) "סביר" else "Moderate"
        else -> Color(0xFFD32F2F) to if (isHebrew) "רועש" else "Loud"
    }

    val displayName = if (isHebrew) place.nameHe else place.nameEn
    val displayType = if (isHebrew) place.typeHe else place.typeEn

    val icon = when (place.typeEn) {
        "Fast Food", "Restaurant" -> Icons.Default.Restaurant
        "Coffee Shop" -> Icons.Default.LocalCafe
        else -> Icons.Default.Store
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = statusColor)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(text = displayType, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${decibels}dB", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = statusColor)
                    Text(text = " • $statusText", fontSize = 14.sp, color = statusColor)
                }
            }

            // Report button for community feedback
            IconButton(
                onClick = onReportClick,
                modifier = Modifier
                    .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                Icon(Icons.Default.Campaign, contentDescription = "Report", tint = Color(0xFF001F3F))
            }
        }
    }
}

@Composable
fun ReportNoiseDialog(place: PlaceData, isHebrew: Boolean, onDismiss: () -> Unit, onSubmit: (Int) -> Unit) {
    val title = if (isHebrew) "דיווח על רעש ב-${place.nameHe}" else "Report noise at ${place.nameEn}"
    val quietBtn = if (isHebrew) "שקט כאן (20%)" else "Quiet (20%)"
    val modBtn = if (isHebrew) "עומס סביר (50%)" else "Moderate (50%)"
    val loudBtn = if (isHebrew) "רועש מאוד! (95%)" else "Very Loud! (95%)"
    val cancelBtn = if (isHebrew) "ביטול" else "Cancel"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(if (isHebrew) "מה רמת העומס והרעש במקום כרגע?" else "What is the current noise level?") },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onSubmit(20) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) { Text(quietBtn) }

                Button(
                    onClick = { onSubmit(50) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) { Text(modBtn) }

                Button(
                    onClick = { onSubmit(95) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(loudBtn) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelBtn, color = Color.Gray)
            }
        },
        containerColor = Color.White
    )
}