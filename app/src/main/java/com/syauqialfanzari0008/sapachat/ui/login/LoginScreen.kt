package com.syauqialfanzari0008.sapachat.ui.login

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {

        YellowChatHeader() // Memanggil header kuning

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Text(text = "Sign in", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Email", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("email@contoh.com", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Password", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("****************", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    Text(
                        text = if (passwordVisible) "Tutup" else "Lihat",
                        modifier = Modifier
                            .clickable { passwordVisible = !passwordVisible }
                            .padding(end = 12.dp),
                        fontSize = 12.sp, color = Color.Gray
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
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
                                    db.collection("Users").document(userId).get()
                                        .addOnSuccessListener { document ->
                                            isLoading = false
                                            if (document.exists()) {
                                                val role = document.getString("role")
                                                val isBanned = document.getBoolean("isBanned") ?: false

                                                if (isBanned && role != "Admin") {
                                                    auth.signOut()
                                                    Toast.makeText(context, "Akun Anda telah dinonaktifkan oleh Admin.", Toast.LENGTH_LONG).show()
                                                } else {
                                                    if (role == "Admin") onNavigateToAdminHome() else onNavigateToUserHome()
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
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B2B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isLoading) "Memeriksa..." else "Sign in", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("Don't have an account? ", fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                Text(
                    text = "Sign up here.", fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }
        }
    }
}

// --- KOMPONEN HEADER KUNING (JANGAN DIHAPUS, DIPAKAI OLEH REGISTER JUGA) ---
@Composable
fun YellowChatHeader() {
    Box(modifier = Modifier.fillMaxWidth().height(280.dp).background(Color(0xFFFCD557))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 85f
            for (x in 0..size.width.toInt() step step.toInt()) drawLine(Color.Black.copy(alpha = 0.1f), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 2f)
            for (y in 0..size.height.toInt() step step.toInt()) drawLine(Color.Black.copy(alpha = 0.1f), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 2f)
        }
        Box(modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 60.dp)) {
            Surface(color = Color.White, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 12.dp, start = 12.dp).border(1.dp, Color.Black, RoundedCornerShape(12.dp))) {
                Text("Hola!\nNice to hear from you.", modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            PinIcon(modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp))
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 40.dp)) {
            Surface(color = Color(0xFF98C7F0), shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 12.dp, start = 12.dp).border(1.dp, Color.Black, RoundedCornerShape(12.dp))) {
                Text("And you, too!", modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            PinIcon(modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp))
        }
    }
}

@Composable
fun PinIcon(modifier: Modifier = Modifier) {
    Surface(color = Color(0xFF3B7BBF), shape = RoundedCornerShape(percent = 50), modifier = modifier.size(24.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(6.dp)) { drawCircle(Color.White) }
            Canvas(modifier = Modifier.size(24.dp)) { drawLine(Color.White, Offset(size.width / 2, size.height / 2), Offset(size.width / 2, size.height), 4f) }
        }
    }
}