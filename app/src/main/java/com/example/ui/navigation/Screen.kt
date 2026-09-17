package com.example.ui.navigation

sealed class Screen(val route: String) {
    object RoleSelection : Screen("role_selection")
    object ShopperDashboard : Screen("shopper_dashboard")
    object GuardDashboard : Screen("guard_dashboard")
    object AdminDashboard : Screen("admin_dashboard")
}
