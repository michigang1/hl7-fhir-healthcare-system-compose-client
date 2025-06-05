package ui.scaffold

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.local.db.datasource.DiagnosisLocalDataSource
import data.local.db.datasource.EventLocalDataSource
import data.local.db.datasource.MedicationLocalDataSource
import data.local.db.datasource.PatientLocalDataSource
import data.model.JwtResponse
import data.remote.RetrofitClient
import data.repository.impl.DiagnosisRepositoryImpl
import data.repository.impl.EventRepositoryImpl
import data.repository.impl.MedicationRepositoryImpl
import data.repository.impl.PatientRepositoryImpl
import data.sync.SynchronizationManager
import ui.navigation.AppScreen
import presentation.*
import presentation.viewmodel.AuditViewModel
import presentation.viewmodel.DiagnosisViewModel
import presentation.viewmodel.EventViewModel
import presentation.viewmodel.MedicationViewModel
import presentation.viewmodel.PatientViewModel
import presentation.viewmodel.CarePlanViewModel
import ui.components.NavButton
import ui.components.NetworkStatusBar
import ui.components.PlaceholderScreen
import ui.screens.DashboardScreen
import ui.screens.JournalScreen
import ui.screens.patient.PatientsScreen
import ui.screens.patient.CarePlanScreen
import utils.TokenManager
import utils.UserManager

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
    synchronizationManager: SynchronizationManager? = null,
    onNavigateToAppScreen: (AppScreen) -> Unit,
    onLogout: () -> Unit
) {
    // Set the user in the UserManager
    UserManager.setUser(jwtResponse)

    var currentActiveAppScreen by remember { mutableStateOf(initialAppScreen) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Add NetworkStatusBar if synchronizationManager is provided
        synchronizationManager?.let { syncManager ->
            NetworkStatusBar(
                isNetworkAvailable = syncManager.isNetworkAvailable,
                syncStatus = syncManager.syncStatus,
                lastSyncTime = syncManager.lastSyncTime,
                onSyncClick = { syncManager.triggerSynchronization() }
            )
        }

        Scaffold(
            modifier = Modifier.weight(1f),
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
                synchronizationManager = synchronizationManager,
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )
        }
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
                UserManager.clearUser()
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
    synchronizationManager: SynchronizationManager? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        when (currentScreen) {
            AppScreen.DASHBOARD -> DashboardScreen(
                carePlanViewModel = CarePlanViewModel(
                    RetrofitClient.carePlanApiService,
                    RetrofitClient.patientApiService
                ),
                eventViewModel = EventViewModel(
                    EventRepositoryImpl(
                        RetrofitClient.eventApiService,
                        EventLocalDataSource()
                    ),
                    synchronizationManager ?: SynchronizationManager(
                        patientRepository = PatientRepositoryImpl(
                            RetrofitClient.patientApiService,
                            PatientLocalDataSource()
                        ),
                        diagnosisRepository = DiagnosisRepositoryImpl(
                            RetrofitClient.diagnosisApiService,
                            DiagnosisLocalDataSource()
                        ),
                        medicationRepository = MedicationRepositoryImpl(
                            RetrofitClient.medicationApiService,
                            MedicationLocalDataSource()
                        ),
                        eventRepository = EventRepositoryImpl(
                            RetrofitClient.eventApiService,
                            EventLocalDataSource()
                        )
                    )
                )
            )
            AppScreen.PATIENTS -> {
                // Create or use the provided SynchronizationManager
                val syncManager = synchronizationManager ?: SynchronizationManager(
                    patientRepository = PatientRepositoryImpl(
                        RetrofitClient.patientApiService,
                        PatientLocalDataSource()
                    ),
                    diagnosisRepository = DiagnosisRepositoryImpl(
                        RetrofitClient.diagnosisApiService,
                        DiagnosisLocalDataSource()
                    ),
                    medicationRepository = MedicationRepositoryImpl(
                        RetrofitClient.medicationApiService,
                        MedicationLocalDataSource()
                    ),
                    eventRepository = EventRepositoryImpl(
                        RetrofitClient.eventApiService,
                        EventLocalDataSource()
                    )
                )

                PatientsScreen(
                    PatientViewModel(
                        PatientRepositoryImpl(
                            RetrofitClient.patientApiService,
                            PatientLocalDataSource()
                        ),
                        syncManager
                    ),
                    MedicationViewModel(
                        MedicationRepositoryImpl(
                            RetrofitClient.medicationApiService,
                            MedicationLocalDataSource()
                        ),
                        syncManager
                    ),
                    DiagnosisViewModel(
                        DiagnosisRepositoryImpl(
                            RetrofitClient.diagnosisApiService,
                            DiagnosisLocalDataSource()
                        ),
                        syncManager
                    )
                )
            }
            AppScreen.PROFILE -> PlaceholderScreen("User Profile Screen")
            AppScreen.CAREPLANS -> {
                // Create or use the provided SynchronizationManager
                val syncManager = synchronizationManager ?: SynchronizationManager(
                    patientRepository = PatientRepositoryImpl(
                        RetrofitClient.patientApiService,
                        PatientLocalDataSource()
                    ),
                    diagnosisRepository = DiagnosisRepositoryImpl(
                        RetrofitClient.diagnosisApiService,
                        DiagnosisLocalDataSource()
                    ),
                    medicationRepository = MedicationRepositoryImpl(
                        RetrofitClient.medicationApiService,
                        MedicationLocalDataSource()
                    ),
                    eventRepository = EventRepositoryImpl(
                        RetrofitClient.eventApiService,
                        EventLocalDataSource()
                    )
                )

                // Get the first patient ID for demonstration purposes
                // In a real app, you might want to pass a selected patient ID
                val patientViewModel = PatientViewModel(
                    PatientRepositoryImpl(
                        RetrofitClient.patientApiService,
                        PatientLocalDataSource()
                    ),
                    syncManager
                )
                val carePlanViewModel = CarePlanViewModel(
                    RetrofitClient.carePlanApiService,
                    RetrofitClient.patientApiService
                )

                // For now, we'll use a placeholder patient ID
                // This should be replaced with actual patient selection logic
                val patientId = 1L

                CarePlanScreen(
                    patientViewModel = patientViewModel,
                    carePlanViewModel = carePlanViewModel,
                    patientId = patientId
                )
            }
            AppScreen.JOURNAL -> {
                // Create and remember a single instance of AuditViewModel
                val auditViewModel = remember { 
                    AuditViewModel(RetrofitClient.auditApiService) 
                }
                JournalScreen(auditViewModel = auditViewModel)
            }
        }
    }
}
