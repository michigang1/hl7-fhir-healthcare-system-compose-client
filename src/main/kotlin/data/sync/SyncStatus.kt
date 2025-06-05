package data.sync

/**
 * Enum representing the different synchronization states.
 */
enum class SyncStatus {
    /**
     * No synchronization is in progress.
     */
    IDLE,
    
    /**
     * Synchronization is in progress.
     */
    SYNCING,
    
    /**
     * Synchronization completed successfully.
     */
    COMPLETED,
    
    /**
     * Synchronization failed.
     */
    FAILED,
    
    /**
     * Network is not available, synchronization is not possible.
     */
    OFFLINE
}