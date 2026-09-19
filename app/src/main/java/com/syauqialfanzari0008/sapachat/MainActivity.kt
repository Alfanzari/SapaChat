package com.syauqialfanzari0008.sapachat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.syauqialfanzari0008.sapachat.ui.login.LoginScreen
import com.syauqialfanzari0008.sapachat.ui.theme.SapaChatTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.syauqialfanzari0008.sapachat.ui.login.RegisterScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SapaChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SapaChatNavigation() // Panggil fungsi navigasinya di sini
                }
            }
        }
    }
}

@Composable
fun SapaChatNavigation() {
    val navController = rememberNavController()

    // NavHost adalah wadah untuk layarmu, startDestination menentukan layar pertama
    NavHost(navController = navController, startDestination = "login") {

        // Rute untuk layar Login
        composable("login") {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        // Rute untuk layar Register
        composable("register") {
            RegisterScreen(
                onNavigateToLogin = {
                    // Kembali ke login dan bersihkan tumpukan layar sebelumnya
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SapaChatTheme {
        Greeting("Android")
    }
}