package com.syauqialfanzari0008.sapachat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth

// Import layar-layar yang ada di aplikasi SapaChat
import com.syauqialfanzari0008.sapachat.ui.login.WelcomeScreen
import com.syauqialfanzari0008.sapachat.ui.login.LoginScreen
import com.syauqialfanzari0008.sapachat.ui.login.RegisterScreen
import com.syauqialfanzari0008.sapachat.ui.login.UserHomeScreen
import com.syauqialfanzari0008.sapachat.ui.login.AdminHomeScreen
import com.syauqialfanzari0008.sapachat.ui.login.ChatScreen
import com.syauqialfanzari0008.sapachat.ui.login.SettingsScreen
import com.syauqialfanzari0008.sapachat.ui.login.EditProfileScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inisialisasi Cloudinary untuk upload gambar profil secara gratis
        val config = HashMap<String, String>()
        config["cloud_name"] = "b5nyzswj" // Sesuai dengan Cloud name Cloudinary kamu
        try {
            com.cloudinary.android.MediaManager.init(this, config)
        } catch (e: Exception) {
            // Mencegah error jika terinisialisasi ulang
        }

        setContent {
            SapaChatNavigation()
        }
    }
}

@Composable
fun SapaChatNavigation() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()

    // CEK SESI LOGIN: Jika ada akun tersimpan, langsung arahkan ke Beranda (user_home)
    val startDest = if (auth.currentUser != null) "user_home" else "welcome"

    NavHost(navController = navController, startDestination = startDest) {

        // 0. Rute Layar Selamat Datang (Welcome Screen)
        composable("welcome") {
            WelcomeScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToLogin = { navController.navigate("login") }
            )
        }

        // Rute Layar Pengaturan (Settings)
        composable("settings") {
            SettingsScreen(
                onNavigateToHome = {
                    navController.navigate("user_home") {
                        popUpTo("user_home") { inclusive = true }
                    }
                },
                onNavigateToEditProfile = {
                    navController.navigate("edit_profile")
                },
                onLogout = {
                    navController.navigate("welcome") {
                        popUpTo(0)
                    }
                }
            )
        }

        // Rute Layar Edit Profile (Terhubung ke Cloudinary & Firestore)
        composable("edit_profile") {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveProfile = { firstName, lastName, dob, email, newImageUri ->
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                        fun saveUserData(imageUrl: String = "") {
                            val userMap = mutableMapOf<String, Any>(
                                "firstName" to firstName,
                                "lastName" to lastName,
                                "dateOfBirth" to dob,
                                "email" to email
                            )
                            if (imageUrl.isNotEmpty()) {
                                userMap["profileImageUrl"] = imageUrl
                            }

                            db.collection("Users").document(userId)
                                .set(userMap, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener {
                                    navController.popBackStack()
                                }
                        }

                        if (newImageUri != null) {
                            com.cloudinary.android.MediaManager.get().upload(newImageUri)
                                .unsigned("ml_default12")
                                .callback(object : com.cloudinary.android.callback.UploadCallback {
                                    override fun onStart(requestId: String) {}
                                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                                        val downloadUrl = resultData["secure_url"].toString()
                                        saveUserData(downloadUrl)
                                    }
                                    override fun onError(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {
                                        saveUserData()
                                    }
                                    override fun onReschedule(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {}
                                }).dispatch()
                        } else {
                            saveUserData()
                        }
                    }
                }
            )
        }

        // 1. Rute Layar Login
        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToUserHome = {
                    navController.navigate("user_home") {
                        popUpTo(0)
                    }
                },
                onNavigateToAdminHome = {
                    navController.navigate("admin_home") {
                        popUpTo(0)
                    }
                }
            )
        }

        // 2. Rute Layar Register
        composable("register") {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }

        // 3. Rute Layar Beranda User
        composable("user_home") {
            UserHomeScreen(
                onNavigateToSettings = {
                    navController.navigate("settings")
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
                    navController.navigate("welcome") {
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