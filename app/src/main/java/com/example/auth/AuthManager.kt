package com.example.auth

import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class UserRole {
    SHOPPER, GUARD, ADMIN, NONE
}

class AuthManager {
    private val auth = Firebase.auth
    
    private val _currentRole = MutableStateFlow(UserRole.NONE)
    val currentRole: StateFlow<UserRole> = _currentRole
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    // Check if user is already signed in via Firebase
    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _isLoggedIn.value = user != null
            // For a real app, you'd fetch their role from Firestore. 
            // In demo mode, we just stay in NONE until they select a demo role.
        }
    }

    fun loginAsDemo(role: UserRole) {
        _currentRole.value = role
        _isLoggedIn.value = true
    }

    fun signOut() {
        auth.signOut()
        _currentRole.value = UserRole.NONE
        _isLoggedIn.value = false
    }
}

val appAuthManager by lazy { AuthManager() }
