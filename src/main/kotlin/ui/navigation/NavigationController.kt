package ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import data.model.JwtResponse
import data.remote.services.AuthApiService
import presentation.viewmodel.LoginViewModel
import presentation.viewmodel.SignUpViewModel


/**
 * NavigationController handles the navigation logic for the application.
 * This separates the navigation logic from the UI components.
 */
class NavigationController(
    private val authApiService: AuthApiService
) {
    // Create a mutable state to hold the current screen
    private val _currentScreen = mutableStateOf<Screen>(Screen.Login)
    val currentScreen: State<Screen> = _currentScreen

    // Navigate to a new screen
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // Create view models with navigation callbacks
    @Composable
    fun createLoginViewModel(): LoginViewModel {
        return remember {
            LoginViewModel(
                apiService = authApiService,
                onLoginSuccess = { jwt ->
                    // Upon successful login, navigate to MainApplication with default screen (DASHBOARD)
                    navigateTo(Screen.MainApplication(jwt, AppScreen.DASHBOARD))
                }
            )
        }
    }

    @Composable
    fun createSignUpViewModel(): SignUpViewModel {
        return remember {
            SignUpViewModel(
                apiService = authApiService,
                onSignUpSuccess = { signUpResponse ->
                    println("Registration successful: ${signUpResponse.message}")
                    navigateTo(Screen.Login) // After registration, go to login screen
                }
            )
        }
    }

    // Navigation callbacks for different screens
    fun onNavigateToRegister() {
        navigateTo(Screen.Register)
    }

    fun onNavigateToLogin() {
        navigateTo(Screen.Login)
    }

    fun onNavigateToAppScreen(currentJwtResponse: JwtResponse, newAppScreen: AppScreen) {
        navigateTo(Screen.MainApplication(currentJwtResponse, newAppScreen))
    }

    fun onLogout() {
        navigateTo(Screen.Login)
    }
}