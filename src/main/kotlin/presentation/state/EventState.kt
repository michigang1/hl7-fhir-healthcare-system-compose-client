package presentation.state

import data.model.EventDto
import data.sync.SyncStatus

/**
 * Represents the state of the events section.
 */
data class EventState(
    val events: List<EventDto> = emptyList(),
    val selectedEvent: EventDto? = null,
    val isEditing: Boolean = false,
    val draftEvent: EventDto? = null,
    val showAddOrEditDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentPatientId: Long? = null, // for context
    val isNetworkAvailable: Boolean = true,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val hasPendingChanges: Boolean = false,
    val showSyncNotification: Boolean = false
)
