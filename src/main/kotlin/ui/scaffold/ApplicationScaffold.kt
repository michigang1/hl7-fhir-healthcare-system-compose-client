package ui.scaffold

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.model.JwtResponse
import data.remote.RetrofitClient
import ui.navigation.AppScreen
import presentation.*
import presentation.viewmodel.DiagnosisViewModel
import presentation.viewmodel.MedicationViewModel
import presentation.viewmodel.PatientViewModel
import ui.components.NavButton
import ui.components.PlaceholderScreen
import ui.screens.patient.PatientsScreen
import utils.TokenManager

/**
 * Main application scaffold that provides the structure for the application after login.
 * Includes top bar with navigation, bottom bar with user info, and content area.
 *
 * @param jwtResponse The JWT response containing the user's authentication token
 * @param initialAppScreen The initial app screen to display
 * @param onNavigateToAppScreen Callback when navigating to a different app screen
 * @param onLogout Callback when the user logs out
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApplicationScaffold(
    jwtResponse: JwtResponse,
    initialAppScreen: AppScreen,
    onNavigateToAppScreen: (AppScreen) -> Unit,
    onLogout: () -> Unit
) {
    var currentActiveAppScreen by remember { mutableStateOf(initialAppScreen) }

    Scaffold(
        topBar = {
            ApplicationTopBar(
                currentScreen = currentActiveAppScreen,
                onScreenSelected = { selectedScreen ->
                    currentActiveAppScreen = selectedScreen
                    onNavigateToAppScreen(selectedScreen)
                },
                onLogout = onLogout
            )
        },
        bottomBar = {
            ApplicationBottomBar(jwtResponse = jwtResponse)
        }
    ) { innerPadding ->
        ApplicationContent(
            currentScreen = currentActiveAppScreen,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        )
    }
}

/**
 * Top app bar with navigation buttons for different app screens and logout button.
 *
 * @param currentScreen The currently active app screen
 * @param onScreenSelected Callback when a screen is selected
 * @param onLogout Callback when the user logs out
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplicationTopBar(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    onLogout: () -> Unit
) {
    TopAppBar(
        title = { Text("Healthcare App - ${currentScreen.title}") },
        actions = {
            // Navigation buttons for each AppScreen
            AppScreen.values().forEach { appScreen ->
                NavButton(
                    label = appScreen.title,
                    screen = appScreen,
                    current = currentScreen,
                    onClick = onScreenSelected
                )
            }
            Spacer(Modifier.width(16.dp))
            OutlinedButton(onClick = {
                TokenManager.clearToken()
                onLogout()
            }) { Text("Logout") }
        }
    )
}

/**
 * Bottom app bar displaying user information.
 *
 * @param jwtResponse The JWT response containing the user's authentication token
 */
@Composable
private fun ApplicationBottomBar(jwtResponse: JwtResponse) {
    BottomAppBar {
        Text(
            "Logged-in token: ${jwtResponse.token.take(10)}...",
            modifier = Modifier.padding(8.dp)
        )
    }
}

/**
 * Main content area that displays the appropriate screen based on the current app screen.
 *
 * @param currentScreen The currently active app screen
 * @param modifier Modifier to be applied to the content
 */
@Composable
private fun ApplicationContent(
    currentScreen: AppScreen,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        when (currentScreen) {
            AppScreen.DASHBOARD -> DashboardScreen()
            AppScreen.PATIENTS -> PatientsScreen(
                PatientViewModel(RetrofitClient.patientApiService),
                MedicationViewModel(RetrofitClient.medicationApiService),
                DiagnosisViewModel(RetrofitClient.diagnosisApiService)
            )
            AppScreen.PROFILE -> PlaceholderScreen("User Profile Screen")
            // Add other screens here
        }
    }
}