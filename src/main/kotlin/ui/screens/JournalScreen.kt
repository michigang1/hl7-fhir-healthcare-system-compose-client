package ui.screens

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
    // Load audit events for the current date when the screen is first displayed
    LaunchedEffect(Unit) {
        auditViewModel.loadAuditEventsForDate(LocalDate.now())
    }

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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Format time - extract only the time portion (HH:mm:ss) from the timestamp
        // Use eventDate which is the creation time of the audit event
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault())
        val formattedTime = timeFormatter.format(event.eventDate)

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
