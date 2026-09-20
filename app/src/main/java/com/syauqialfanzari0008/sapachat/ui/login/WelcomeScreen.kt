package com.syauqialfanzari0008.sapachat.ui.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {

        // --- BAGIAN ATAS: Latar Kotak-kotak (Grid) & Gelembung Chat ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f) // Proporsi tinggi bagian atas
                .background(Color(0xFF222222)) // Warna latar gelap
        ) {
            // 1. Menggambar Background Grid
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val step = 90f // Lebar kotak grid

                // Garis Vertikal
                for (x in 0..canvasWidth.toInt() step step.toInt()) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(x.toFloat(), 0f),
                        end = Offset(x.toFloat(), canvasHeight),
                        strokeWidth = 2f
                    )
                }
                // Garis Horizontal
                for (y in 0..canvasHeight.toInt() step step.toInt()) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, y.toFloat()),
                        end = Offset(canvasWidth, y.toFloat()),
                        strokeWidth = 2f
                    )
                }
            }

            // 2. Meletakkan Gelembung Chat Melayang
            ChatBubbleGraphic(
                text = "Hola!\nNice to hear from you.",
                bgColor = Color(0xFFFFCC66),
                modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 56.dp)
            )
            ChatBubbleGraphic(
                text = "And you, too!",
                bgColor = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 24.dp, top = 140.dp)
            )
            ChatBubbleGraphic(
                text = "Let's meet?\n\uD83C\uDF7A 8 at J's?",
                bgColor = Color(0xFFFF8844),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp, top = 40.dp)
            )
            ChatBubbleGraphic(
                text = "\uD83D\uDC4D See you!",
                bgColor = Color(0xFF88CCFF),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 40.dp, bottom = 48.dp)
            )
        }

        // --- BAGIAN BAWAH: Teks & Tombol Aksi ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Proporsi tinggi bagian bawah
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "#SapaChat", // Saya ubah dari #Checked agar sesuai nama aplikasimu
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Talk, text, and share as much as you\nwant — all of it for free.",
                    fontSize = 16.sp,
                    color = Color.DarkGray,
                    lineHeight = 24.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tombol Get Started (Ke Register)
                Button(
                    onClick = onNavigateToRegister,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Teks Login
                Text(
                    text = "I already have an account.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier
                        .clickable { onNavigateToLogin() }
                        .padding(8.dp)
                )
            }
        }
    }
}

// Komponen khusus untuk menggambar desain gelembung beserta ikon jarum pin
@Composable
fun ChatBubbleGraphic(text: String, bgColor: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(top = 10.dp, start = 10.dp) // Ruang agar pin tidak tertutup
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = Color.Black,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }

        // Simulasi Ikon Pin Biru di sudut kiri atas
        Surface(
            color = Color(0xFF4A7D9E),
            shape = RoundedCornerShape(percent = 50),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(6.dp)) { drawCircle(Color.White) }
            }
        }
    }
}