package com.example.auth

import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UserRole {
    SHOPPER, GUARD, ADMIN, NONE
}

data class UserProfile(
    val email: String,
    val displayName: String,
    val role: UserRole = UserRole.SHOPPER,
    val loyaltyTier: String = "Gold Member",
    val loyaltyPoints: Int = 350,
    val memberDiscountPercent: Int = 5
)

class AuthManager {
    private val auth by lazy {
        try {
            Firebase.auth
        } catch (e: Exception) {
            null
        }
    }
    
    private val _currentRole = MutableStateFlow(UserRole.NONE)
    val currentRole: StateFlow<UserRole> = _currentRole
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    // Check if user is already signed in via Firebase
    init {
        try {
            auth?.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    _isLoggedIn.value = true
                    _currentUser.value = UserProfile(
                        email = user.email ?: "shopper@scanandgo.store",
                        displayName = user.displayName ?: "Scan & Go Shopper",
                        role = _currentRole.value.takeIf { it != UserRole.NONE } ?: UserRole.SHOPPER
                    )
                } else if (_currentRole.value == UserRole.NONE) {
                    _isLoggedIn.value = false
                    _currentUser.value = null
                }
            }
        } catch (_: Exception) {
            // Safe fallback if Firebase is not fully configured in test env
        }
    }

    fun loginUser(email: String, displayName: String = "", role: UserRole = UserRole.SHOPPER) {
        val name = if (displayName.isNotBlank()) displayName else email.substringBefore("@").replaceFirstChar { it.uppercase() }
        _currentRole.value = role
        _isLoggedIn.value = true
        _currentUser.value = UserProfile(
            email = email,
            displayName = name,
            role = role,
            loyaltyTier = if (role == UserRole.SHOPPER) "Gold Member" else "Staff",
            loyaltyPoints = 420,
            memberDiscountPercent = 5
        )
    }

    fun loginAsDemo(role: UserRole) {
        _currentRole.value = role
        _isLoggedIn.value = true
        _currentUser.value = when (role) {
            UserRole.SHOPPER -> UserProfile(
                email = "shopper@scanandgo.store",
                displayName = "Alex Rivera",
                role = UserRole.SHOPPER,
                loyaltyTier = "Gold Member",
                loyaltyPoints = 350,
                memberDiscountPercent = 5
            )
            UserRole.GUARD -> UserProfile(
                email = "guard@scanandgo.store",
                displayName = "Officer Vance",
                role = UserRole.GUARD,
                loyaltyTier = "Security Tier 2",
                loyaltyPoints = 0,
                memberDiscountPercent = 0
            )
            UserRole.ADMIN -> UserProfile(
                email = "admin@scanandgo.store",
                displayName = "Store Admin",
                role = UserRole.ADMIN,
                loyaltyTier = "Superuser",
                loyaltyPoints = 0,
                memberDiscountPercent = 0
            )
            UserRole.NONE -> null
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (_: Exception) {}
        _currentRole.value = UserRole.NONE
        _isLoggedIn.value = false
        _currentUser.value = null
    }
}

val appAuthManager by lazy { AuthManager() }
