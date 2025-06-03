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
            val eventTypeFromData = data.entries.joinToString(", ") { (key, value) ->
                if (value != null) "$key: $value" else key
            }
            return if (eventTypeFromData.isNotEmpty()) eventTypeFromData else eventTypeRaw
        }
}
