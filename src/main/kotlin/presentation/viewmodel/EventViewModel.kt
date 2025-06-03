package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.model.EventDto
import data.model.EventRequest
import data.model.EventResponse
import data.remote.services.EventApiService
import kotlinx.coroutines.*
import presentation.state.EventState
import java.io.IOException
import java.time.LocalDateTime

/**
 * ViewModel for managing events.
 */
class EventViewModel(
    private val eventApiService: EventApiService,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<EventState>(mainDispatcher, ioDispatcher) {
    override var state by mutableStateOf(EventState())

    /**
     * Loads all events.
     */
    fun loadAllEvents() {
        launchCoroutine {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val events = fetchAndMapEvents()
                state = state.copy(
                    events = events,
                    isLoading = false
                )
            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Error loading events: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Loads events for a specific patient.
     */
    fun loadEventsForPatient(patientId: Long) {
        state = state.copy(currentPatientId = patientId, isLoading = true, errorMessage = null)
        launchCoroutine {
            try {
                val events = fetchAndMapEventsForPatient(patientId)
                state = state.copy(
                    events = events,
                    isLoading = false
                )
            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Error loading events for patient: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Fetches and maps all events from the API.
     */
    private suspend fun fetchAndMapEvents(): List<EventDto> {
        val response = withContext(ioDispatcher) {
            eventApiService.getAllEvents()
        }
        if (response.isSuccessful) {
            return response.body()?.map { EventDto.fromEventResponse(it) }?.sortedBy { it.eventDateTime } ?: emptyList()
        } else {
            throw IOException("Error loading events: ${response.code()} ${response.message()}")
        }
    }

    /**
     * Fetches and maps events for a specific patient from the API.
     */
    private suspend fun fetchAndMapEventsForPatient(patientId: Long): List<EventDto> {
        val response = withContext(ioDispatcher) {
            eventApiService.getEventsByPatient(patientId)
        }
        if (response.isSuccessful) {
            return response.body()?.map { EventDto.fromEventResponse(it) }?.sortedBy { it.eventDateTime } ?: emptyList()
        } else {
            throw IOException("Error loading events for patient: ${response.code()} ${response.message()}")
        }
    }

    /**
     * Opens the dialog for adding a new event.
     */
    fun openAddEventDialog(patientId: Long) {
        // Create an empty EventDto with default values
        val emptyEvent = EventDto(
            id = 0L,
            name = "",
            description = "",
            authorId = 1L, // Default author ID
            authorUsername = "User", // Default username
            eventDateTime = java.time.LocalDateTime.now(), // Current date and time
            patients = emptyList() // Empty list of patients
        )

        state = state.copy(
            showAddOrEditDialog = true,
            isEditing = false,
            draftEvent = emptyEvent,
            selectedEvent = null,
            currentPatientId = patientId
        )
    }

    /**
     * Opens the dialog for editing an existing event.
     */
    fun openEditEventDialog(event: EventDto) {
        state = state.copy(
            showAddOrEditDialog = true,
            isEditing = true,
            draftEvent = event,
            selectedEvent = event
        )
    }

    /**
     * Closes the add/edit event dialog.
     */
    fun closeAddOrEditEventDialog() {
        state = state.copy(
            showAddOrEditDialog = false,
            isEditing = false,
            draftEvent = null
        )
    }

    /**
     * Updates the draft event.
     */
    fun updateDraftEvent(updater: (EventDto) -> EventDto) {
        state.draftEvent?.let {
            state = state.copy(draftEvent = updater(it))
        }
    }

    /**
     * Saves the current draft event.
     */
    fun saveEvent() {
        val eventToSave = state.draftEvent ?: return
        val patientId = state.currentPatientId ?: return

        launchCoroutine {
            executeApiCall(
                loadingState = { state.copy(isLoading = true, errorMessage = null) },
                errorState = { error -> state.copy(errorMessage = "Error saving event: $error", isLoading = false) },
                apiCall = {
                    if (state.isEditing) {
                        // Create EventRequest from EventDto
                        val eventRequest = EventRequest(
                            name = eventToSave.name,
                            description = eventToSave.description,
                            authorId = eventToSave.authorId,
                            eventDateTime = eventToSave.eventDateTime,
                            patientIds = eventToSave.patients.map { it.id }.toSet()
                        )
                        eventApiService.updateEvent(eventToSave.id, eventRequest)
                    } else {
                        // Create EventRequest from EventDto
                        val eventRequest = EventRequest(
                            name = eventToSave.name,
                            description = eventToSave.description,
                            authorId = eventToSave.authorId,
                            eventDateTime = eventToSave.eventDateTime,
                            patientIds = eventToSave.patients.map { it.id }.toSet()
                        )
                        eventApiService.createEvent(patientId, eventRequest)
                    }
                },
                onSuccess = { eventResponse ->
                    // Refresh events
                    if (state.currentPatientId != null) {
                        loadEventsForPatient(state.currentPatientId!!)
                    } else {
                        loadAllEvents()
                    }
                    state.copy(
                        isLoading = false,
                        showAddOrEditDialog = false,
                        isEditing = false,
                        draftEvent = null,
                        selectedEvent = EventDto.fromEventResponse(eventResponse)
                    )
                }
            )
        }
    }

    /**
     * Deletes an event.
     */
    fun deleteEvent(eventId: Long) {
        launchCoroutine {
            executeApiCall(
                loadingState = { state.copy(isLoading = true, errorMessage = null) },
                errorState = { error -> state.copy(errorMessage = "Error deleting event: $error", isLoading = false) },
                apiCall = { eventApiService.deleteEvent(eventId) },
                onSuccess = { success ->
                    // Refresh events
                    if (state.currentPatientId != null) {
                        loadEventsForPatient(state.currentPatientId!!)
                    } else {
                        loadAllEvents()
                    }
                    state.copy(
                        isLoading = false,
                        selectedEvent = if (state.selectedEvent?.id == eventId) null else state.selectedEvent,
                        draftEvent = null,
                        isEditing = false
                    )
                }
            )
        }
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
