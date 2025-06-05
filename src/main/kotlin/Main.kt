import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import data.local.db.DatabaseInitializer
import data.local.db.datasource.DiagnosisLocalDataSource
import data.local.db.datasource.EventLocalDataSource
import data.local.db.datasource.MedicationLocalDataSource
import data.local.db.datasource.PatientLocalDataSource
import data.remote.RetrofitClient
import data.repository.impl.DiagnosisRepositoryImpl
import data.repository.impl.EventRepositoryImpl
import data.repository.impl.MedicationRepositoryImpl
import data.repository.impl.PatientRepositoryImpl
import data.sync.SynchronizationManager
import ui.navigation.NavigationController
import ui.navigation.AppNavigation

/**
 * Main entry point for the Healthcare System Compose Client application.
 * 
 * Sets up the application window, creates the navigation controller,
 * and initializes the UI with the appropriate navigation components.
 */
fun main() = application {
    // Initialize the database
    DatabaseInitializer.initialize()

    // Create repositories
    val patientRepository = PatientRepositoryImpl(
        RetrofitClient.patientApiService,
        PatientLocalDataSource()
    )

    val diagnosisRepository = DiagnosisRepositoryImpl(
        RetrofitClient.diagnosisApiService,
        DiagnosisLocalDataSource()
    )

    val medicationRepository = MedicationRepositoryImpl(
        RetrofitClient.medicationApiService,
        MedicationLocalDataSource()
    )

    val eventResponse = EventRepositoryImpl(RetrofitClient.eventApiService, EventLocalDataSource())

    // Create and start the synchronization manager
    val synchronizationManager = remember {
        SynchronizationManager(
            patientRepository = patientRepository,
            diagnosisRepository = diagnosisRepository,
            medicationRepository = medicationRepository,
            eventRepository = eventResponse
        )
    }

    // Start network monitoring
    LaunchedEffect(synchronizationManager) {
        synchronizationManager.startNetworkMonitoring()
    }

    // Clean up when the application is closed
    DisposableEffect(synchronizationManager) {
        onDispose {
            synchronizationManager.cleanup()
            DatabaseInitializer.closeDatabase()
        }
    }

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
                navigationController = navigationController,
                synchronizationManager = synchronizationManager
            )
        }
    }
}
