package data.sync

import data.repository.PatientRepository
import data.repository.DiagnosisRepository
import data.repository.EventRepository
import data.repository.MedicationRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import utils.NetworkConnectivityMonitor
import utils.NetworkUtils
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manager for synchronizing local data with the remote server.
 * This class is responsible for:
 * 1. Monitoring network connectivity
 * 2. Triggering synchronization when the network becomes available
 * 3. Providing a way to manually trigger synchronization
 * 4. Reporting synchronization status
 */
class SynchronizationManager(
    private val patientRepository: PatientRepository,
    private val diagnosisRepository: DiagnosisRepository,
    private val medicationRepository: MedicationRepository,
    private val eventRepository: EventRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())
    private val isSynchronizing = AtomicBoolean(false)
    private var networkMonitorJob: Job? = null
    private var isMonitoringActive = false

    private val networkConnectivityMonitor = NetworkConnectivityMonitor()

    // Synchronization status
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    // Last sync time
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    // Network status
    val isNetworkAvailable = networkConnectivityMonitor.isNetworkAvailable

    /**
     * Starts monitoring network connectivity and triggers synchronization when the network becomes available.
     */
    fun startNetworkMonitoring() {
        if (isMonitoringActive) return

        isMonitoringActive = true

        // Start the network connectivity monitor
        networkConnectivityMonitor.startMonitoring()

        // Monitor network status changes
        networkMonitorJob = coroutineScope.launch {
            networkConnectivityMonitor.isNetworkAvailable.collect { isAvailable ->
                if (isAvailable) {
                    // Network became available, trigger sync if not already syncing
                    if (!isSynchronizing.get()) {
                        synchronizeAll()
                    }
                } else {
                    // Network is not available
                    _syncStatus.value = SyncStatus.OFFLINE
                }
            }
        }
    }

    /**
     * Stops monitoring network connectivity.
     */
    fun stopNetworkMonitoring() {
        isMonitoringActive = false
        networkConnectivityMonitor.stopMonitoring()
        networkMonitorJob?.cancel()
        networkMonitorJob = null
    }

    /**
     * Synchronizes all repositories with the remote server.
     * 
     * @return True if all synchronizations were successful, false otherwise
     */
    suspend fun synchronizeAll(): Boolean {
        println("[DEBUG] SynchronizationManager.synchronizeAll() called")
        if (isSynchronizing.getAndSet(true)) {
            println("[DEBUG] Already synchronizing, returning false")
            return false // Already synchronizing
        }

        _syncStatus.value = SyncStatus.SYNCING
        println("[DEBUG] Set syncStatus to SYNCING")

        try {
            if (!NetworkUtils.isNetworkAvailable()) {
                println("[DEBUG] Network not available, returning false")
                _syncStatus.value = SyncStatus.OFFLINE
                return false
            }

            var allSuccessful = true
            println("[DEBUG] Starting synchronization of all repositories")

            // Synchronize all repositories
            println("[DEBUG] Calling patientRepository.synchronize()")
            val patientSync = patientRepository.synchronize()
            println("[DEBUG] patientRepository.synchronize() returned $patientSync")
            if (!patientSync) allSuccessful = false

            println("[DEBUG] Calling diagnosisRepository.synchronize()")
            val diagnosisSync = diagnosisRepository.synchronize()
            println("[DEBUG] diagnosisRepository.synchronize() returned $diagnosisSync")
            if (!diagnosisSync) allSuccessful = false

            println("[DEBUG] Calling medicationRepository.synchronize()")
            val medicationSync = medicationRepository.synchronize()
            println("[DEBUG] medicationRepository.synchronize() returned $medicationSync")
            if (!medicationSync) allSuccessful = false

            println("[DEBUG] Calling eventRepository.synchronize()")
            val eventSync = eventRepository.synchronize()
            println("[DEBUG] eventRepository.synchronize() returned $eventSync")
            if (!eventSync) allSuccessful = false

            // Update sync status and last sync time
            if (allSuccessful) {
                _syncStatus.value = SyncStatus.COMPLETED
                _lastSyncTime.value = System.currentTimeMillis()
            } else {
                _syncStatus.value = SyncStatus.FAILED
            }

            return allSuccessful
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.FAILED
            return false
        } finally {
            isSynchronizing.set(false)
        }
    }

    /**
     * Manually triggers synchronization.
     * 
     * @return True if synchronization was successful, false otherwise
     */
    fun triggerSynchronization() {
        println("[DEBUG] SynchronizationManager.triggerSynchronization() called")
        if (isSynchronizing.get()) {
            println("[DEBUG] Already synchronizing, returning")
            return
        }

        if (!NetworkUtils.isNetworkAvailable()) {
            println("[DEBUG] Network not available, setting status to OFFLINE and returning")
            _syncStatus.value = SyncStatus.OFFLINE
            return
        }

        println("[DEBUG] Launching coroutine to synchronize")
        coroutineScope.launch {
            println("[DEBUG] Inside coroutine, setting status to SYNCING")
            _syncStatus.value = SyncStatus.SYNCING
            println("[DEBUG] Calling synchronizeAll()")
            val success = synchronizeAll()
            println("[DEBUG] synchronizeAll() returned $success")
            if (success) {
                println("[DEBUG] Synchronization successful, setting status to COMPLETED")
                _syncStatus.value = SyncStatus.COMPLETED
                _lastSyncTime.value = System.currentTimeMillis()
            } else {
                println("[DEBUG] Synchronization failed, setting status to FAILED")
                _syncStatus.value = SyncStatus.FAILED
            }
        }
    }

    /**
     * Checks if synchronization is currently in progress.
     * 
     * @return True if synchronizing, false otherwise
     */
    fun isSynchronizing(): Boolean {
        return isSynchronizing.get()
    }

    /**
     * Cleans up resources when the manager is no longer needed.
     */
    fun cleanup() {
        stopNetworkMonitoring()
        coroutineScope.cancel()
    }
}
