package ui.navigation

import androidx.compose.runtime.Composable
import ui.navigation.NavigationController
import ui.screens.auth.LoginScreen
import ui.screens.auth.SignUpScreen
import ui.scaffold.MainApplicationScaffold

/**
 * Main navigation component that handles switching between different screens based on the current navigation state.
 * 
 * @param currentScreen The current screen to display
 * @param navigationController The navigation controller that manages navigation state and actions
 */
@Composable
fun AppNavigation(
    currentScreen: Screen,
    navigationController: NavigationController
) {
    when (currentScreen) {
        is Screen.Login -> {
            val loginViewModel = navigationController.createLoginViewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToRegister = { navigationController.onNavigateToRegister() }
            )
        }

        is Screen.Register -> {
            val signUpViewModel = navigationController.createSignUpViewModel()
            SignUpScreen(
                viewModel = signUpViewModel,
                onNavigateToLogin = { navigationController.onNavigateToLogin() },
                onSignUpSuccessNavigation = {
                    // Navigate to login screen after successful registration
                    navigationController.onNavigateToLogin()
                }
            )
        }

        is Screen.MainApplication -> {
            // User is logged in, showing the main interface with Scaffold and TopAppBar
            MainApplicationScaffold(
                jwtResponse = currentScreen.jwtResponse,
                initialAppScreen = currentScreen.currentAppScreen,
                onNavigateToAppScreen = { newAppScreen ->
                    // Update MainApplication state with the newly selected AppScreen
                    navigationController.onNavigateToAppScreen(currentScreen.jwtResponse, newAppScreen)
                },
                onLogout = {
                    navigationController.onLogout() // Return to login screen on logout
                }
            )
        }
    }
}