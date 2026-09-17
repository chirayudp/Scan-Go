package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.auth.UserRole
import com.example.auth.appAuthManager
import com.example.ui.navigation.Screen
import com.example.ui.screens.admin.AdminScreen
import com.example.ui.screens.auth.RoleSelectionScreen
import com.example.ui.screens.guard.GuardScreen
import com.example.ui.screens.shopper.ShopperScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val role by appAuthManager.currentRole.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.RoleSelection.route
    ) {
        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(onRoleSelected = { selectedRole ->
                val route = when (selectedRole) {
                    UserRole.SHOPPER -> Screen.ShopperDashboard.route
                    UserRole.GUARD -> Screen.GuardDashboard.route
                    UserRole.ADMIN -> Screen.AdminDashboard.route
                    else -> Screen.RoleSelection.route
                }
                navController.navigate(route) {
                    popUpTo(Screen.RoleSelection.route) { inclusive = true }
                }
            })
        }
        composable(Screen.ShopperDashboard.route) {
            ShopperScreen()
        }
        composable(Screen.GuardDashboard.route) {
            GuardScreen()
        }
        composable(Screen.AdminDashboard.route) {
            AdminScreen()
        }
    }
}
