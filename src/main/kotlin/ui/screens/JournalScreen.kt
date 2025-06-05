package ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.AuditEvent
import presentation.viewmodel.AuditViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun JournalScreen(auditViewModel: AuditViewModel) {
    // The ViewModel now automatically loads events for the current date when created

    val state = auditViewModel.state

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Date navigation controls
        DateNavigationBar(
            selectedDate = state.selectedDate,
            onPreviousDay = { auditViewModel.navigateToPreviousDay() },
            onNextDay = { auditViewModel.navigateToNextDay() }
        )

        Spacer(Modifier.height(16.dp))

        // Audit events table
        AuditEventsTable(
            auditEvents = state.auditEvents,
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }

    // Show error message if needed
    state.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { auditViewModel.clearErrorMessage() },
            title = { Text("Error") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { auditViewModel.clearErrorMessage() }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun DateNavigationBar(
    selectedDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousDay) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowLeft,
                contentDescription = "Previous Day"
            )
        }

        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
            style = MaterialTheme.typography.titleMedium
        )

        IconButton(onClick = onNextDay) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = "Next Day"
            )
        }
    }
}

@Composable
private fun AuditEventsTable(
    auditEvents: List<AuditEvent>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Audit Journal", style = MaterialTheme.typography.titleMedium)
            Divider(Modifier.padding(vertical = 8.dp))

            // Table header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Time",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(0.2f)
                )
                Text(
                    "Principal",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(0.3f)
                )
                Text(
                    "Event Type",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(0.5f)
                )
            }

            Divider(Modifier.padding(vertical = 8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (auditEvents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No audit events for this day.")
                }
            } else {
                LazyColumn {
                    items(auditEvents) { event ->
                        AuditEventRow(event = event)
                        Divider(Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditEventRow(event: AuditEvent) {
    // Define colors for successful and unsuccessful events
    val successColor = Color(0x1F00C853) // Light green with 12% opacity
    val failureColor = Color(0x1FFF5252) // Light red with 12% opacity

    // Determine background color based on event success/failure status
    val backgroundColor = if (event.isSuccess) successColor else failureColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = backgroundColor),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Format time - extract only the time portion (HH:mm:ss) from the timestamp
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault())

        // Try to use eventTypeRaw as timestamp if it's a valid timestamp
        val timestamp = if (event.eventTypeRaw.matches(Regex("\\d{4}-\\d{2}-\\d{2}T.*"))) {
            try {
                Instant.parse(event.eventTypeRaw)
            } catch (e: Exception) {
                // Fall back to eventDate if parsing fails
                event.eventDate
            }
        } else {
            event.eventDate
        }

        val formattedTime = timeFormatter.format(timestamp)

        Text(
            formattedTime,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.2f)
        )
        Text(
            event.principal,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.3f)
        )
        // Display the event type (which is extracted from data or falls back to raw value)
        Text(
            event.eventType,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.5f)
        )
    }
}
