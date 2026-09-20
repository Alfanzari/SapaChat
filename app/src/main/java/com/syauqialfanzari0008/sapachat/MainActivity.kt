package com.syauqialfanzari0008.sapachat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.syauqialfanzari0008.sapachat.ui.login.WelcomeScreen
import com.syauqialfanzari0008.sapachat.ui.login.LoginScreen
import com.syauqialfanzari0008.sapachat.ui.login.RegisterScreen
import com.syauqialfanzari0008.sapachat.ui.login.UserHomeScreen
import com.syauqialfanzari0008.sapachat.ui.login.AdminHomeScreen
import com.syauqialfanzari0008.sapachat.ui.login.ChatScreen
import com.syauqialfanzari0008.sapachat.ui.login.SettingsScreen
import com.syauqialfanzari0008.sapachat.ui.login.EditProfileScreen
import com.syauqialfanzari0008.sapachat.ui.login.FeedScreen

class MainActivity : ComponentActivity() {


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Izin notifikasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }


        val config = HashMap<String, String>()
        config["cloud_name"] = "b5nyzswj"
        try {
            com.cloudinary.android.MediaManager.init(this, config)
        } catch (e: Exception) {

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
    val db = FirebaseFirestore.getInstance()


    LaunchedEffect(auth.currentUser) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    db.collection("Users").document(userId).update("fcmToken", token)
                }
            }
        }
    }

    val startDest = if (auth.currentUser != null) "user_home" else "welcome"

    NavHost(navController = navController, startDestination = startDest) {

        composable("welcome") {
            WelcomeScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToLogin = { navController.navigate("login") }
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateToHome = {
                    navController.navigate("user_home") {
                        popUpTo("user_home") { inclusive = false }
                    }
                },
                onNavigateToFeed = {
                    navController.navigate("feed") {
                        popUpTo("user_home") { saveState = true }
                        restoreState = true
                    }
                },
                onNavigateToEditProfile = {
                    navController.navigate("edit_profile")
                },
                onLogout = {
                    navController.navigate("welcome") { popUpTo(0) }
                }
            )
        }

        composable("edit_profile") {
            val context = androidx.compose.ui.platform.LocalContext.current

            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveProfile = { firstName, lastName, dob, username, email, newImageUri ->
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        val formattedUsername = username.trim().lowercase().replace(" ", "")

                        fun saveUserData(imageUrl: String = "") {
                            val userMap = mutableMapOf<String, Any>(
                                "firstName" to firstName,
                                "lastName" to lastName,
                                "dateOfBirth" to dob,
                                "username" to formattedUsername,
                                "email" to email
                            )
                            if (imageUrl.isNotEmpty()) {
                                userMap["profileImageUrl"] = imageUrl
                            }

                            db.collection("Users").document(userId)
                                .set(userMap, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                        }

                        fun uploadImageAndSave() {
                            if (newImageUri != null) {
                                com.cloudinary.android.MediaManager.get().upload(newImageUri)
                                    .unsigned("ml_default12")
                                    .callback(object : com.cloudinary.android.callback.UploadCallback {
                                        override fun onStart(requestId: String) {}
                                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                                            saveUserData(resultData["secure_url"].toString())
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

                        if (formattedUsername.isNotEmpty()) {
                            db.collection("Users")
                                .whereEqualTo("username", formattedUsername)
                                .get()
                                .addOnSuccessListener { documents ->
                                    val isTakenByOther = documents.any { it.id != userId }
                                    if (isTakenByOther) {
                                        Toast.makeText(context, "Username @$formattedUsername sudah dipakai pengguna lain!", Toast.LENGTH_LONG).show()
                                    } else {
                                        uploadImageAndSave()
                                    }
                                }
                        } else {
                            uploadImageAndSave()
                        }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToUserHome = { navController.navigate("user_home") { popUpTo(0) } },
                onNavigateToAdminHome = { navController.navigate("admin_home") { popUpTo(0) } }
            )
        }

        composable("register") {
            RegisterScreen(
                onNavigateToLogin = { navController.navigate("login") { popUpTo("register") { inclusive = true } } }
            )
        }

        composable("user_home") {
            UserHomeScreen(
                onNavigateToFeed = {
                    navController.navigate("feed") { popUpTo("user_home") { saveState = true }; restoreState = true }
                },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToChat = { uid, email -> navController.navigate("chat/$uid/$email") }
            )
        }

        composable("feed") {
            FeedScreen(
                onNavigateToHome = { navController.navigate("user_home") { popUpTo("user_home") { inclusive = false } } },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }

        composable("admin_home") {
            AdminHomeScreen(
                onLogout = { navController.navigate("welcome") { popUpTo(0) } },
                onNavigateToChat = { uid, email -> navController.navigate("chat/$uid/$email") }
            )
        }

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