package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import data.sync.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * A bar that displays the current network status and synchronization progress.
 *
 * @param isNetworkAvailable Flow of network availability status
 * @param syncStatus Flow of synchronization status
 * @param lastSyncTime Flow of the last successful synchronization time
 * @param onSyncClick Callback when the sync button is clicked
 */
@Composable
fun NetworkStatusBar(
    isNetworkAvailable: Flow<Boolean>,
    syncStatus: StateFlow<SyncStatus>,
    lastSyncTime: StateFlow<Long?>,
    onSyncClick: () -> Unit
) {
    val networkAvailable by isNetworkAvailable.collectAsState(initial = false)
    val currentSyncStatus by syncStatus.collectAsState()
    val lastSync by lastSyncTime.collectAsState()

    val backgroundColor = when {
        !networkAvailable -> Color(0xFFFFCDD2) // Light red for offline
        currentSyncStatus == SyncStatus.FAILED -> Color(0xFFFFF9C4) // Light yellow for sync failed
        else -> Color(0xFFE8F5E9) // Light green for online
    }

    val statusText = when {
        !networkAvailable -> "Offline Mode"
        currentSyncStatus == SyncStatus.SYNCING -> "Synchronizing..."
        currentSyncStatus == SyncStatus.COMPLETED -> "Synchronized"
        currentSyncStatus == SyncStatus.FAILED -> "Sync Failed"
        else -> "Online"
    }

    val icon = when {
        !networkAvailable -> Icons.Default.CloudOff
        currentSyncStatus == SyncStatus.SYNCING -> Icons.Default.CloudSync
        currentSyncStatus == SyncStatus.FAILED -> Icons.Default.Error
        else -> Icons.Default.Done
    }

    val lastSyncText = lastSync?.let {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        "Last sync: ${sdf.format(Date(it))}"
    } ?: "Never synced"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = statusText,
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = lastSyncText,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        Button(
            onClick = onSyncClick,
            enabled = networkAvailable && currentSyncStatus != SyncStatus.SYNCING,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text("Sync Now")
        }
    }
}
