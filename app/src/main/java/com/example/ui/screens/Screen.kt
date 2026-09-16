package com.example.ui.screens

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Clients : Screen("clients")
    object Reports : Screen("reports")
}
