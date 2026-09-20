package com.syauqialfanzari0008.sapachat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.syauqialfanzari0008.sapachat.ui.login.LoginScreen
import com.syauqialfanzari0008.sapachat.ui.login.RegisterScreen
import com.syauqialfanzari0008.sapachat.ui.login.UserHomeScreen
import com.syauqialfanzari0008.sapachat.ui.login.AdminHomeScreen
import com.syauqialfanzari0008.sapachat.ui.login.ChatScreen
// Tambahkan import theme kamu jika ada, misalnya: import com.syauqialfanzari0008.sapachat.ui.theme.SapaChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Kita langsung panggil pengatur rute utama di sini
            SapaChatNavigation()
        }
    }
}

@Composable
fun SapaChatNavigation() {
    val navController = rememberNavController()

    // startDestination = "login" memastikan layar pertama yang dibuka adalah halaman Login
    NavHost(navController = navController, startDestination = "login") {

        // 1. Rute Layar Login
        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToUserHome = {
                    navController.navigate("user_home") {
                        popUpTo("login") { inclusive = true } // Cegah user bisa 'back' ke halaman login setelah masuk
                    }
                },
                onNavigateToAdminHome = {
                    navController.navigate("admin_home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // 2. Rute Layar Register
        composable("register") {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // 3. Rute Layar Beranda User (Daftar Kontak)
        composable("user_home") {
            UserHomeScreen(
                onLogout = {
                    // SEKARANG, saat logout ditekan, aplikasi akan pindah ke layar login
                    navController.navigate("login") {
                        popUpTo(0) // Menghapus semua riwayat layar sebelumnya
                    }
                },
                onNavigateToChat = { uid, email ->
                    navController.navigate("chat/$uid/$email")
                }
            )
        }

        // 4. Rute Layar Beranda Admin
        composable("admin_home") {
            AdminHomeScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                },
                onNavigateToChat = { uid, email ->
                    navController.navigate("chat/$uid/$email")
                }
            )
        }

        // 5. Rute Layar Chat Room Privat
        composable("chat/{uid}/{email}") { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            val email = backStackEntry.arguments?.getString("email") ?: ""

            ChatScreen(
                receiverUid = uid,
                receiverEmail = email,
                onBack = { navController.popBackStack() }
            )
        }
    }
}