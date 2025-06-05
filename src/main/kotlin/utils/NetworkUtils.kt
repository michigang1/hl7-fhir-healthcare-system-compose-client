package utils

import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.io.IOException

/**
 * Utility class for network-related operations.
 */
object NetworkUtils {
    private const val CONNECTION_TIMEOUT_MS = 1500
    private const val DEFAULT_TEST_URL = "http://localhost:8080" // Same host as the API server
    
    /**
     * Checks if the network is available by attempting to connect to the API server.
     * 
     * @return true if the network is available, false otherwise
     */
    fun isNetworkAvailable(): Boolean {
        return try {
            val socket = Socket()
            val url = URL(DEFAULT_TEST_URL)
            val socketAddress = InetSocketAddress(url.host, url.port.takeIf { it != -1 } ?: 80)
            
            socket.connect(socketAddress, CONNECTION_TIMEOUT_MS)
            socket.close()
            
            true
        } catch (e: IOException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}