import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import data.remote.RetrofitClient
import ui.navigation.NavigationController
import ui.navigation.AppNavigation

/**
 * Main entry point for the Healthcare System Compose Client application.
 * 
 * Sets up the application window, creates the navigation controller,
 * and initializes the UI with the appropriate navigation components.
 */
fun main() = application {
    // Create the navigation controller
    val authApiService = RetrofitClient.authApiService
    val navigationController = remember { NavigationController(authApiService) }

    // Get the current screen from the navigation controller
    val currentScreen by navigationController.currentScreen

    Window(
        onCloseRequest = ::exitApplication,
        title = "Healthcare Client"
    ) {
        MaterialTheme { // Make sure MaterialTheme wraps your application
            AppNavigation(
                currentScreen = currentScreen,
                navigationController = navigationController
            )
        }
    }
}
