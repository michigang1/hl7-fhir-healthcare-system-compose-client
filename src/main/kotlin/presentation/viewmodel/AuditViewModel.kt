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

    /**
     * Loads audit events for the selected date.
     */
    fun loadAuditEventsForDate(date: LocalDate) {
        state = state.copy(selectedDate = date, isLoading = true, errorMessage = null)

        // Calculate start and end of the day in Instant
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        launchCoroutine {
            try {
                val events = fetchAuditEvents()

                // Filter events for the selected date
                // Include events that occur on the selected date (between start of day and end of day)
                val filteredEvents = events.filter { event ->
                    val eventDate = event.eventDate
                    eventDate.compareTo(startOfDay) >= 0 && eventDate.compareTo(endOfDay) < 0
                }

                // Sort events by time (ascending order)
                val sortedEvents = filteredEvents.sortedBy { it.eventDate }

                state = state.copy(
                    auditEvents = sortedEvents,
                    isLoading = false
                )
            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Error loading audit events: ${e.message}",
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
