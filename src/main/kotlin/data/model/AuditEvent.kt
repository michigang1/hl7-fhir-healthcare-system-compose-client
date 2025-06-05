package data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import data.serializers.InstantSerializer

@Serializable
data class AuditEvent(
    @SerialName("timestamp")
    @Serializable(with = InstantSerializer::class)
    val eventDate: Instant,
    val principal: String,
    @SerialName("type")
    val eventTypeRaw: String, // This is actually a timestamp in the response
    val data: Map<String, String?> = mapOf()
) {
    // Generate a unique ID based on timestamp and principal
    val id: Long
        get() = eventDate.toEpochMilli() + principal.hashCode()

    // Extract the actual event type from the data field or use the raw value as fallback
    val eventType: String
        get() {
            // Try to extract event type from data
            // Handle the new format where keys are enclosed in curly braces
            val eventTypeFromData = data.entries.joinToString(", ") { (key, value) ->
                // Extract the actual event type from the key if it's enclosed in curly braces
                val actualKey = if (key.startsWith("{") && key.endsWith("}")) {
                    key.substring(1, key.length - 1)
                } else {
                    key
                }

                if (value != null) "$actualKey: $value" else actualKey
            }
            return if (eventTypeFromData.isNotEmpty()) eventTypeFromData else eventTypeRaw
        }

    // Determine if the event represents a successful operation
    val isSuccess: Boolean
        get() {
            // Check if the event type contains "SUCCESS" and doesn't contain "FAILURE"
            return (eventType.contains("SUCCESS", ignoreCase = true) || 
                   eventType.contains("success", ignoreCase = true)) && 
                   !eventType.contains("FAILURE", ignoreCase = true) &&
                   !eventType.contains("failure", ignoreCase = true)
        }
}
