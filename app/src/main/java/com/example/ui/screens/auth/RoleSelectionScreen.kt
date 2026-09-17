package com.example.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.auth.UserRole
import com.example.auth.appAuthManager

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (UserRole) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Scan & Go",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "Select your role to continue",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
            )

            RoleButton("Continue as Shopper", UserRole.SHOPPER, onRoleSelected)
            RoleButton("Continue as Guard", UserRole.GUARD, onRoleSelected)
            RoleButton("Continue as Admin", UserRole.ADMIN, onRoleSelected)
        }
    }
}

@Composable
fun RoleButton(text: String, role: UserRole, onClick: (UserRole) -> Unit) {
    Button(
        onClick = { 
            appAuthManager.loginAsDemo(role)
            onClick(role) 
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(56.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}
