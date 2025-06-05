package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.model.AuditEvent
import data.remote.services.AuditApiService
import kotlinx.coroutines.*
import presentation.state.AuditState
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * ViewModel for managing audit events.
 */
class AuditViewModel(
    private val auditApiService: AuditApiService,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<AuditState>(mainDispatcher, ioDispatcher) {
    override var state by mutableStateOf(AuditState())

    // Store all audit events
    private var allAuditEvents: List<AuditEvent> = emptyList()

    init {
        // Load all audit events when the ViewModel is created
        loadAllAuditEvents()
    }

    /**
     * Loads all audit events from the API.
     */
    private fun loadAllAuditEvents() {
        state = state.copy(isLoading = true, errorMessage = null)

        launchCoroutine {
            try {
                // Fetch events and remove duplicates
                val events = fetchAuditEvents()

                // Remove duplicate events by considering a combination of fields that uniquely identify an event
                allAuditEvents = events.distinctBy { event -> 
                    // Create a composite key using the relevant fields
                    Triple(
                        // Use eventTypeRaw as timestamp if it's valid, otherwise use eventDate
                        if (event.eventTypeRaw.matches(Regex("\\d{4}-\\d{2}-\\d{2}T.*"))) {
                            try {
                                Instant.parse(event.eventTypeRaw).toEpochMilli()
                            } catch (e: Exception) {
                                event.eventDate.toEpochMilli()
                            }
                        } else {
                            event.eventDate.toEpochMilli()
                        },
                        event.principal,
                        event.eventType
                    )
                }

                // After loading all events, filter for the current date
                loadAuditEventsForDate(state.selectedDate)
            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Error loading audit events: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Loads audit events for the selected date.
     */
    fun loadAuditEventsForDate(date: LocalDate) {
        state = state.copy(selectedDate = date, isLoading = true, errorMessage = null)

        // No need to fetch events again, just filter the stored events
        launchCoroutine {
            try {
                // Filter events for the selected date
                val filteredEvents = allAuditEvents.filter { event ->
                    // First try to parse eventTypeRaw as a timestamp if it looks like one
                    if (event.eventTypeRaw.matches(Regex("\\d{4}-\\d{2}-\\d{2}T.*"))) {
                        try {
                            val rawTimestamp = Instant.parse(event.eventTypeRaw)
                            val rawLocalDate = rawTimestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                            return@filter rawLocalDate == date
                        } catch (e: Exception) {
                            // If parsing fails, fall back to eventDate
                        }
                    }

                    // Fall back to eventDate if eventTypeRaw is not a valid timestamp
                    val eventLocalDate = event.eventDate.atZone(ZoneId.systemDefault()).toLocalDate()
                    eventLocalDate == date
                }

                // Sort events by time (ascending order)
                val sortedEvents = filteredEvents.sortedBy { event -> 
                    // Try to use eventTypeRaw as timestamp for sorting if it's a valid timestamp
                    if (event.eventTypeRaw.matches(Regex("\\d{4}-\\d{2}-\\d{2}T.*"))) {
                        try {
                            return@sortedBy Instant.parse(event.eventTypeRaw)
                        } catch (e: Exception) {
                            // Fall back to eventDate if parsing fails
                        }
                    }
                    event.eventDate
                }

                state = state.copy(
                    auditEvents = sortedEvents,
                    isLoading = false
                )
            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Error filtering audit events: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Fetches audit events from the API.
     */
    private suspend fun fetchAuditEvents(): List<AuditEvent> {
        val response = withContext(ioDispatcher) {
            auditApiService.getAuditEvents()
        }
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw IOException("Error loading audit events: ${response.code()} ${response.message()}")
        }
    }

    /**
     * Navigates to the previous day.
     */
    fun navigateToPreviousDay() {
        val previousDay = state.selectedDate.minusDays(1)
        loadAuditEventsForDate(previousDay)
    }

    /**
     * Navigates to the next day.
     */
    fun navigateToNextDay() {
        val nextDay = state.selectedDate.plusDays(1)
        loadAuditEventsForDate(nextDay)
    }

    /**
     * Clears the error message.
     */
    fun clearErrorMessage() {
        state = state.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
    }
}
