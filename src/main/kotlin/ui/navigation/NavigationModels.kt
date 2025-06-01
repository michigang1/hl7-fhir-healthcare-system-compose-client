package ui.navigation

import data.model.JwtResponse

/**
 * Defines screens available after login
 */
enum class AppScreen(val title: String) {
    DASHBOARD("Dashboard"),
    PATIENTS("Patients"), // Example of additional screen
    PROFILE("Profile")    // Another example
}

/**
 * Sealed class for navigation
 */
sealed class Screen {
    object Login : Screen()
    object Register : Screen()
    // Store JWT and current active screen inside the main application
    data class MainApplication(
        val jwtResponse: JwtResponse,
        val currentAppScreen: AppScreen = AppScreen.DASHBOARD // Default screen after login
    ) : Screen()
}