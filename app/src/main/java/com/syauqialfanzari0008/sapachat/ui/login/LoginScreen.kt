package com.syauqialfanzari0008.sapachat.ui.login

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToUserHome: () -> Unit,
    onNavigateToAdminHome: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Masuk SapaChat",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    isLoading = true
                    auth.signInWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid ?: ""

                                // Tahan user sebentar, cek statusnya di Firestore
                                db.collection("Users").document(userId).get()
                                    .addOnSuccessListener { document ->
                                        isLoading = false
                                        if (document.exists()) {
                                            val role = document.getString("role")
                                            val isBanned = document.getBoolean("isBanned") ?: false

                                            // PENOLAKAN AKUN YANG DIBLOKIR
                                            if (isBanned && role != "Admin") {
                                                auth.signOut() // Tendang keluar dari sistem auth
                                                Toast.makeText(context, "Akun Anda telah dinonaktifkan oleh Admin.", Toast.LENGTH_LONG).show()
                                            } else {
                                                // Izinkan masuk sesuai peran
                                                if (role == "Admin") {
                                                    onNavigateToAdminHome()
                                                } else {
                                                    onNavigateToUserHome()
                                                }
                                            }
                                        } else {
                                            auth.signOut()
                                            Toast.makeText(context, "Data profil tidak ditemukan.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                        auth.signOut()
                                        Toast.makeText(context, "Gagal terhubung ke database.", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                isLoading = false
                                Toast.makeText(context, "Gagal masuk: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Email dan Password tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Memeriksa..." else "Masuk")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToRegister) {
            Text("Belum punya akun? Daftar di sini")
        }
    }
}