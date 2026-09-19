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

@Composable
fun RegisterScreen(onNavigateToLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Daftar Akun Baru",
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

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Konfirmasi Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    if (password == confirmPassword) {
                        // Proses daftar ke Firebase
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Akun berhasil dibuat!", Toast.LENGTH_SHORT).show()
                                    onNavigateToLogin() // Otomatis kembali ke layar login
                                } else {
                                    Toast.makeText(context, "Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "Password tidak cocok!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Email dan Password tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Daftar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("Sudah punya akun? Masuk di sini")
        }
    }
}