package com.syauqialfanzari0008.sapachat.ui.login

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


data class UserProfile(
    val uid: String,
    val email: String,
    val isBanned: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    onLogout: () -> Unit,
    onNavigateToChat: (String, String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection("Users")
            .whereEqualTo("role", "User")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val userList = snapshot.documents.mapNotNull { doc ->
                        val uid = doc.getString("uid")
                        val email = doc.getString("email")
                        val isBanned = doc.getBoolean("isBanned") ?: false

                        if (uid != null && email != null) {
                            UserProfile(uid, email, isBanned)
                        } else null
                    }
                    users = userList
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Admin", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        auth.signOut()
                        onLogout()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Text(
                text = "Manajemen Pengguna",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(users) { user ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { onNavigateToChat(user.uid, user.email) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (user.isBanned) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    color = if (user.isBanned) Color.Red else MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = user.email.take(1).uppercase(),
                                            color = if (user.isBanned) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (user.isBanned) Color.Red else Color.Unspecified
                                    )
                                    Text(
                                        text = if (user.isBanned) "Akun Dinonaktifkan" else "Ketuk untuk balas pesan",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (user.isBanned) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }


                            IconButton(
                                onClick = {
                                    val newStatus = !user.isBanned
                                    db.collection("Users").document(user.uid)
                                        .update("isBanned", newStatus)
                                        .addOnSuccessListener {
                                            val msg = if (newStatus) "Akun diblokir" else "Blokir dibuka"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                }
                            ) {
                                Icon(
                                    imageVector = if (user.isBanned) Icons.Default.CheckCircle else Icons.Default.Block,
                                    contentDescription = "Toggle Ban Status",
                                    tint = if (user.isBanned) Color.Green else Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}