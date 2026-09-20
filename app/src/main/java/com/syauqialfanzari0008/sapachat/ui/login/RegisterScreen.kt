package com.syauqialfanzari0008.sapachat.ui.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun RegisterScreen(onNavigateToLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {

        YellowChatHeader()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Text(text = "Sign up", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.Black)
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
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val userId = auth.currentUser?.uid ?: ""

                                    val userMap = hashMapOf(
                                        "uid" to userId,
                                        "email" to email.trim(),
                                        "role" to "User"
                                    )

                                    db.collection("Users").document(userId)
                                        .set(userMap)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            Toast.makeText(context, "Pendaftaran berhasil!", Toast.LENGTH_SHORT).show()
                                            onNavigateToLogin()
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            Toast.makeText(context, "Gagal simpan data: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Gagal daftar: ${task.exception?.message}", Toast.LENGTH_LONG).show()
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
                Text(if (isLoading) "Mendaftar..." else "Sign up", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("Already have an account? ", fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                Text(
                    text = "Sign in here.", fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }
    }
}