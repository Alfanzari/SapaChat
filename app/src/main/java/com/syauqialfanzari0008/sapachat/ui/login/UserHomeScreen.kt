package com.syauqialfanzari0008.sapachat.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Struktur data sederhana untuk menyimpan info kontak
data class UserData(val uid: String, val email: String, val role: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(onLogout: () -> Unit, onNavigateToChat: (String, String) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    // Menyimpan daftar kontak dan status loading
    var usersList by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Mengambil data dari Firestore saat layar pertama kali dibuka
    LaunchedEffect(Unit) {
        db.collection("Users").get()
            .addOnSuccessListener { snapshot ->
                val users = mutableListOf<UserData>()
                for (document in snapshot.documents) {
                    val uid = document.getString("uid") ?: ""
                    val email = document.getString("email") ?: ""
                    val role = document.getString("role") ?: ""

                    // Logika penting: Jangan masukkan akun kita sendiri ke daftar kontak!
                    if (uid != currentUser?.uid) {
                        users.add(UserData(uid, email, role))
                    }
                }
                usersList = users
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat SapaChat") },
                actions = {
                    Button(
                        onClick = {
                            auth.signOut()
                            onLogout()
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Masuk sebagai: ${currentUser?.email}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                // Tampilan loading bulat saat mengambil data
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (usersList.isEmpty()) {
                // Jika belum ada pengguna lain di database
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada kontak lain yang terdaftar.")
                }
            } else {
                // Menampilkan daftar kontak yang bisa di-scroll
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(usersList) { user ->
                        UserContactItem(user = user) {
                            // Jalankan perintah pindah layar sambil membawa UID dan Email target
                            onNavigateToChat(user.uid, user.email)
                        }
                    }
                }
            }
        }
    }
}

// Desain kartu untuk masing-masing kontak
@Composable
fun UserContactItem(user: UserData, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar berbentuk lingkaran dengan huruf depan email
            Surface(
                modifier = Modifier.size(40.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (user.email.isNotEmpty()) user.email.take(1).uppercase() else "?",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = user.email, fontWeight = FontWeight.Bold)
                Text(text = "Role: ${user.role}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}