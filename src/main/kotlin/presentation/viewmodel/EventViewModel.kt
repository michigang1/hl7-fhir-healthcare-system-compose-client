package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.model.EventDto
import data.model.EventRequest
import data.repository.EventRepository
import data.sync.SynchronizationManager
import data.sync.SyncStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import presentation.state.EventState
import java.io.IOException
import java.time.LocalDateTime

/**
 * ViewModel for managing events.
 */
class EventViewModel(
    private val eventRepository: EventRepository,
    private val synchronizationManager: SynchronizationManager,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<EventState>(mainDispatcher, ioDispatcher) {
    override var state by mutableStateOf(EventState())

    init {
        // Check for pending changes on initialization
        launchCoroutine {
            checkForPendingChanges()
        }

        // Observe network connectivity
        launchCoroutine {
            synchronizationManager.isNetworkAvailable.collectLatest { isAvailable ->
                state = state.copy(isNetworkAvailable = isAvailable)

                // If network becomes available and we have pending changes, show sync notification
                if (isAvailable && state.hasPendingChanges) {
                    state = state.copy(showSyncNotification = true)
                }
            }
        }

        // Observe synchronization status
        launchCoroutine {
            synchronizationManager.syncStatus.collectLatest { status ->
                state = state.copy(syncStatus = status)

                // If sync completed successfully, refresh data and hide notification
                if (status == SyncStatus.COMPLETED) {
                    refreshData()
                    state = state.copy(
                        hasPendingChanges = false,
                        showSyncNotification = false
                    )
                }
            }
        }
    }

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
     * Fetches and maps all events from the repository.
     */
    private suspend fun fetchAndMapEvents(): List<EventDto> {
        return withContext(ioDispatcher) {
            eventRepository.getAllEvents()
        }
    }

    /**
     * Fetches and maps events for a specific patient from the repository.
     */
    private suspend fun fetchAndMapEventsForPatient(patientId: Long): List<EventDto> {
        return withContext(ioDispatcher) {
            eventRepository.getEventsByPatient(patientId)
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
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                // Create EventRequest from EventDto
                val eventRequest = EventRequest(
                    name = eventToSave.name,
                    description = eventToSave.description,
                    authorId = eventToSave.authorId,
                    eventDateTime = eventToSave.eventDateTime,
                    patientIds = eventToSave.patients.map { it.id }.toSet()
                )

                val savedEvent = if (state.isEditing) {
                    eventRepository.updateEvent(eventToSave.id, eventRequest)
                } else {
                    eventRepository.createEvent(patientId, eventRequest)
                }

                // Refresh events
                if (state.currentPatientId != null) {
                    loadEventsForPatient(state.currentPatientId!!)
                } else {
                    loadAllEvents()
                }

                state = state.copy(
                    isLoading = false,
                    showAddOrEditDialog = false,
                    isEditing = false,
                    draftEvent = null,
                    selectedEvent = savedEvent
                )

                // Check for pending changes after successful save
                checkForPendingChanges()
            } catch (e: IOException) {
                state = state.copy(
                    errorMessage = "Network error: ${e.message}",
                    isLoading = false
                )

                // Check for pending changes after network error
                // This is important because the repository will store changes locally
                checkForPendingChanges()
            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Error saving event: ${e.message}",
                    isLoading = false
                )

                // Check for pending changes after other errors
                // This is important because the repository might store changes locally
                checkForPendingChanges()
            }
        }
    }

    /**
     * Deletes an event.
     */
    fun deleteEvent(eventId: Long) {
        launchCoroutine {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val success = eventRepository.deleteEvent(eventId)

                // Refresh events
                if (state.currentPatientId != null) {
                    loadEventsForPatient(state.currentPatientId!!)
                } else {
                    loadAllEvents()
                }

                state = state.copy(
                    isLoading = false,
                    selectedEvent = if (state.selectedEvent?.id == eventId) null else state.selectedEvent,
                    draftEvent = null,
                    isEditing = false
                )

                // Check for pending changes after successful delete
                checkForPendingChanges()
            } catch (e: IOException) {
                state = state.copy(
                    errorMessage = "Network error: ${e.message}",
                    isLoading = false
                )

                // Check for pending changes after network error
                // This is important because the repository will mark for deletion locally
                checkForPendingChanges()
            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Error deleting event: ${e.message}",
                    isLoading = false
                )

                // Check for pending changes after other errors
                // This is important because the repository might mark for deletion locally
                checkForPendingChanges()
            }
        }
    }

    /**
     * Clears the error message.
     */
    fun clearErrorMessage() {
        state = state.copy(errorMessage = null)
    }

    /**
     * Refreshes data based on the current context (all events or events for a specific patient).
     */
    private fun refreshData() {
        if (state.currentPatientId != null) {
            loadEventsForPatient(state.currentPatientId!!)
        } else {
            loadAllEvents()
        }
    }

    /**
     * Manually triggers synchronization.
     */
    fun triggerSynchronization() {
        if (state.syncStatus == SyncStatus.SYNCING) return

        synchronizationManager.triggerSynchronization()
        state = state.copy(showSyncNotification = false)
    }

    /**
     * Dismisses the synchronization notification.
     */
    fun dismissSyncNotification() {
        state = state.copy(showSyncNotification = false)
    }

    /**
     * Checks if there are pending changes that need to be synchronized.
     * This is called after operations that might create pending changes.
     */
    private suspend fun checkForPendingChanges() {
        val eventsToSync = withContext(ioDispatcher) {
            eventRepository.getEventsToSync()
        }

        state = state.copy(
            hasPendingChanges = eventsToSync.isNotEmpty(),
            showSyncNotification = eventsToSync.isNotEmpty() && state.isNetworkAvailable
        )
    }

    override fun onCleared() {
        super.onCleared()
    }
}
