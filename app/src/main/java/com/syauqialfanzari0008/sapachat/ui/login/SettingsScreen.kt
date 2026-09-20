package com.syauqialfanzari0008.sapachat.ui.login

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToFeed: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }


    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("Users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        firstName = document.getString("firstName") ?: ""
                        lastName = document.getString("lastName") ?: ""
                        profileImageUrl = document.getString("profileImageUrl") ?: ""
                    }
                }
        }
    }

    val displayName = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
        "$firstName $lastName".trim()
    } else {
        currentUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User"
    }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8F8F8))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToHome) {
                    Icon(Icons.Filled.ChatBubble, contentDescription = "Chats", tint = Color.Gray, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = onNavigateToFeed) {
                    Icon(Icons.Filled.ViewAgenda, contentDescription = "Feed", tint = Color.Gray, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.Black, modifier = Modifier.size(28.dp))
                }
            }
        },
        containerColor = Color(0xFFF8F8F8)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Settings",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToEditProfile() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6B8BC2)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileImageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = profileImageUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = if (displayName.isNotEmpty()) displayName.take(1).uppercase() else "U",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        Text(text = "Edit your profile", color = Color.Gray, fontSize = 14.sp)
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Edit",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column {

                    SettingsMenuItem(title = "Language") {
                        showLanguageDialog = true
                    }
                    HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)


                    SettingsMenuItem(title = "Notifications") {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                    HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)


                    SettingsMenuItem(title = "Privacy") {
                        showPrivacyDialog = true
                    }
                    HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)


                    SettingsMenuItem(title = "Support") {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@sapachat.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Bantuan SapaChat")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Tidak ada aplikasi email yang terpasang", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    auth.signOut()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Sign out", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }


        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = { Text("Language", fontWeight = FontWeight.Bold) },
                text = { Text("Fitur ganti bahasa akan segera hadir pada pembaruan SapaChat berikutnya!") },
                confirmButton = {
                    TextButton(onClick = { showLanguageDialog = false }) { Text("Oke", color = Color.Black) }
                },
                containerColor = Color.White
            )
        }


        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
                text = { Text("SapaChat sangat menjaga kerahasiaan data Anda. Segala bentuk pertukaran pesan yang dikirim dan diterima hanya dapat diakses oleh Anda dan lawan bicara Anda.") },
                confirmButton = {
                    TextButton(onClick = { showPrivacyDialog = false }) { Text("Tutup", color = Color.Black) }
                },
                containerColor = Color.White
            )
        }
    }
}


@Composable
fun SettingsMenuItem(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.Black
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Go",
            tint = Color.Gray
        )
    }
}