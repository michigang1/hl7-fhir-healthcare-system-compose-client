package utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Monitors network connectivity and provides a flow of connectivity status changes.
 */
class NetworkConnectivityMonitor(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val checkIntervalMs: Long = 5000 // Check every 5 seconds by default
) {
    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())
    private val _isNetworkAvailable = MutableStateFlow(false)

    /**
     * A flow that emits the current network connectivity status.
     * True if the network is available, false otherwise.
     */
    val isNetworkAvailable: Flow<Boolean> = _isNetworkAvailable.asStateFlow()

    private var isMonitoring = false

    /**
     * Starts monitoring network connectivity.
     */
    fun startMonitoring() {
        if (isMonitoring) return

        isMonitoring = true
        coroutineScope.launch {
            while (isActive) {
                val networkAvailable = NetworkUtils.isNetworkAvailable()
                if (_isNetworkAvailable.value != networkAvailable) {
                    _isNetworkAvailable.value = networkAvailable
                }
                delay(checkIntervalMs)
            }
        }
    }

    /**
     * Stops monitoring network connectivity.
     */
    fun stopMonitoring() {
        isMonitoring = false
        coroutineScope.cancel()
    }

    /**
     * Checks if the network is currently available.
     * 
     * @return True if the network is available, false otherwise
     */
    fun isNetworkAvailableNow(): Boolean {
        return NetworkUtils.isNetworkAvailable()
    }
}
