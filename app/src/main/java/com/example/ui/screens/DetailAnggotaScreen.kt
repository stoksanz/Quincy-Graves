package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BatteryIndicator
import com.example.ui.components.GuardianButton
import com.example.ui.components.GuardianHeader
import com.example.ui.components.StatusPill
import com.example.ui.viewmodel.GuardianViewModel

@Composable
fun DetailAnggotaScreen(
    viewModel: GuardianViewModel,
    onBackClick: () -> Unit
) {
    val member by viewModel.selectedMember.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Geofencing & Map Simulation states
    var geofenceRadius by remember { mutableStateOf(100f) } // Radius in visual dp
    var isOutsideGeofence by remember { mutableStateOf(false) }
    var connectionType by remember { mutableStateOf("Koneksi Wi-Fi (Sangat Stabil)") }
    var isRefreshingLocation by remember { mutableStateOf(false) }

    // Pulsing transition for map pin
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val markerPulseRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    // Location history states (simulated path list)
    var locationHistory by remember {
        mutableStateOf(
            listOf(
                "Rumah Utama (Zona Aman)" to "Baru saja",
                "Grand Indonesia Mall" to "1 jam yang lalu",
                "Kawasan Sudirman (SCBD)" to "3 jam yang lalu",
                "Stasiun MRT Bundaran HI" to "5 jam yang lalu"
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            GuardianHeader(
                title = "Detail Anggota",
                subtitle = "Status keamanan dan detail koneksi",
                onBackClick = onBackClick
            )

            member?.let { m ->
                // Avatar and Status Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                if (m.status == "Bahaya 🚨") Color.Red.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.1f
                                ),
                                CircleShape
                            )
                            .border(
                                2.dp,
                                if (m.status == "Bahaya 🚨") Color.Red.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.3f
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = m.name.take(2).uppercase(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (m.status == "Bahaya 🚨") Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = m.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        m.roleInFamily?.let { role ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = role,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Interactive Information Cards
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Status, Location & Battery Info Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Status Keamanan",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Status Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "Status",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Status Koneksi", fontSize = 13.sp)
                                }
                                StatusPill(status = m.status)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Battery Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.BatteryFull,
                                        contentDescription = "Daya Baterai",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Daya Baterai", fontSize = 13.sp)
                                }
                                BatteryIndicator(percentage = m.battery)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Location Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = "Lokasi",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Lokasi Terakhir", fontSize = 13.sp)
                                }
                                Text(
                                    text = m.lastLocationName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Last Seen Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = "Terakhir Dilihat",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Terakhir Dilihat", fontSize = 13.sp)
                                }
                                Text(
                                    text = m.lastSeen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Communication Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Informasi Kontak",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Phone detail
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Nomor HP", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(
                                        text = if (m.phoneNumber.isNotBlank()) m.phoneNumber else "Belum ditambahkan",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (m.phoneNumber.isNotBlank()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${m.phoneNumber}"))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                Icons.Default.Call,
                                                contentDescription = "Telepon",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${m.phoneNumber}"))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                Icons.Default.Message,
                                                contentDescription = "Kirim Pesan",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Emergency contact
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Kontak Darurat", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(
                                        text = if (m.emergencyContact.isNotBlank()) m.emergencyContact else "Belum diatur",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (m.emergencyContact.isNotBlank()) Color.Red else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (m.emergencyContact.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${m.emergencyContact}"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                Color.Red.copy(alpha = 0.1f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            Icons.Default.ContactPhone,
                                            contentDescription = "Hubungi Darurat",
                                            tint = Color.Red,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Peta Realtime & Geofence Simulation Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Peta & Zona Aman (Geofence)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (isOutsideGeofence) "🚨 Keluar dari Zona Aman!" else "🟢 Berada di Zona Aman",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOutsideGeofence) Color.Red else Color(0xFF2E7D32)
                                    )
                                }

                                if (isRefreshingLocation) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Custom High-Fidelity Simulated Vector Map Canvas
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height

                                    // 1. Draw Map Ground
                                    drawRect(color = Color(0xFFECEFF1))

                                    // 2. Draw Parks (Green Zones)
                                    drawCircle(
                                        color = Color(0xFFE0F2F1),
                                        radius = 110f,
                                        center = Offset(w * 0.15f, h * 0.25f)
                                    )
                                    drawCircle(
                                        color = Color(0xFFE8F5E9),
                                        radius = 140f,
                                        center = Offset(w * 0.85f, h * 0.8f)
                                    )

                                    // 3. Draw Water Bodies (Lake)
                                    drawCircle(
                                        color = Color(0xFFE0F7FA),
                                        radius = 90f,
                                        center = Offset(w * 0.75f, h * 0.2f)
                                    )

                                    // 4. Draw Roads & Streets (White lines with grey edges)
                                    // Main Horizontal Road
                                    drawLine(
                                        color = Color.White,
                                        start = Offset(0f, h * 0.5f),
                                        end = Offset(w, h * 0.5f),
                                        strokeWidth = 28f
                                    )
                                    drawLine(
                                        color = Color(0xFFCFD8DC),
                                        start = Offset(0f, h * 0.5f - 14f),
                                        end = Offset(w, h * 0.5f - 14f),
                                        strokeWidth = 1.5f
                                    )
                                    drawLine(
                                        color = Color(0xFFCFD8DC),
                                        start = Offset(0f, h * 0.5f + 14f),
                                        end = Offset(w, h * 0.5f + 14f),
                                        strokeWidth = 1.5f
                                    )

                                    // Diagonal Road
                                    drawLine(
                                        color = Color.White,
                                        start = Offset(0f, h * 0.9f),
                                        end = Offset(w, h * 0.1f),
                                        strokeWidth = 20f
                                    )

                                    // Main Vertical Road
                                    drawLine(
                                        color = Color.White,
                                        start = Offset(w * 0.45f, 0f),
                                        end = Offset(w * 0.45f, h),
                                        strokeWidth = 28f
                                    )
                                    drawLine(
                                        color = Color(0xFFCFD8DC),
                                        start = Offset(w * 0.45f - 14f, 0f),
                                        end = Offset(w * 0.45f - 14f, h),
                                        strokeWidth = 1.5f
                                    )
                                    drawLine(
                                        color = Color(0xFFCFD8DC),
                                        start = Offset(w * 0.45f + 14f, 0f),
                                        end = Offset(w * 0.45f + 14f, h),
                                        strokeWidth = 1.5f
                                    )

                                    // 5. Draw Geofence (Zona Aman) Circle Centered
                                    val geofenceCenter = Offset(w * 0.45f, h * 0.5f)
                                    val visualGeofenceRadius = geofenceRadius * 1.5f
                                    drawCircle(
                                        color = Color(0xFF1E88E5).copy(alpha = 0.12f),
                                        radius = visualGeofenceRadius,
                                        center = geofenceCenter
                                    )
                                    drawCircle(
                                        color = Color(0xFF1E88E5).copy(alpha = 0.5f),
                                        radius = visualGeofenceRadius,
                                        center = geofenceCenter,
                                        style = Stroke(width = 4f)
                                    )

                                    // 6. Member's Current Location Pin
                                    // If simulating danger, move far away out of the circle
                                    val memberPos = if (isOutsideGeofence) {
                                        Offset(w * 0.85f, h * 0.25f)
                                    } else {
                                        Offset(w * 0.42f, h * 0.45f)
                                    }

                                    // Pulsing Background Circle
                                    drawCircle(
                                        color = (if (isOutsideGeofence) Color.Red else Color(0xFF1E88E5)).copy(alpha = 0.25f),
                                        radius = markerPulseRadius,
                                        center = memberPos
                                    )

                                    // Pin Outer
                                    drawCircle(
                                        color = if (isOutsideGeofence) Color.Red else Color(0xFF1E88E5),
                                        radius = 12f,
                                        center = memberPos
                                    )

                                    // Pin Inner
                                    drawCircle(
                                        color = Color.White,
                                        radius = 5f,
                                        center = memberPos
                                    )
                                }

                                // Visual overlay text indicating safe zone label
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                        .border(0.5.dp, Color(0xFF1E88E5).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Zona Aman Utama",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E88E5)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Geofence Radius Slider
                            Text(
                                text = "Radius Zona Aman: ${(geofenceRadius * 3).toInt()} meter",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Slider(
                                value = geofenceRadius,
                                onValueChange = { geofenceRadius = it },
                                valueRange = 50f..200f,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Threat Simulator Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isOutsideGeofence) Color.Red.copy(alpha = 0.05f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Geofence Alert Tracker",
                                        tint = if (isOutsideGeofence) Color.Red else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Simulasi Keluar Zona",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOutsideGeofence) Color.Red else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Simulasikan anggota berjalan keluar",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                Switch(
                                    checked = isOutsideGeofence,
                                    onCheckedChange = { checked ->
                                        isOutsideGeofence = checked
                                        if (checked) {
                                            viewModel.addManualActivity("${m.name} keluar dari Zona Aman Geofence!")
                                        } else {
                                            viewModel.addManualActivity("${m.name} kembali memasuki Zona Aman.")
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Map Actions Row (Refresh & Real Google Maps Direction)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GuardianButton(
                                    text = if (isRefreshingLocation) "Mendeteksi..." else "Refresh Lokasi",
                                    onClick = {
                                        isRefreshingLocation = true
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(1200)
                                            isRefreshingLocation = false
                                            val newSpot = if (isOutsideGeofence) "Grand Indonesia Mall (Luar Geofence)" else "Kawasan Rumah Utama"
                                            locationHistory = listOf(newSpot to "Baru saja") + locationHistory
                                        }
                                    },
                                    enabled = !isRefreshingLocation,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = {
                                        val geoUri = "geo:-6.200000,106.816666?q=-6.200000,106.816666(Lokasi ${m.name})"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            RoundedCornerShape(14.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Navigation,
                                        contentDescription = "Buka Google Maps",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Location History Timeline Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Riwayat Perjalanan Lokasi",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Loop of Timeline Steps
                            locationHistory.take(4).forEachIndexed { index, pair ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                ) {
                                    // Timeline vertical line column
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(20.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(
                                                    if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.3f
                                                    ),
                                                    CircleShape
                                                )
                                        )
                                        if (index < locationHistory.size - 1) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(28.dp)
                                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = pair.first,
                                            fontSize = 13.sp,
                                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                                            color = if (index == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = pair.second,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
