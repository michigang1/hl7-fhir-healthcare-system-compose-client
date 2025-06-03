package data.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import data.serializers.LocalDateTimeSerializer

@Serializable
data class EventRequest(
    val name: String,
    val description: String,
    val authorId: Long,
    @Serializable(with = LocalDateTimeSerializer::class)
    val eventDateTime: LocalDateTime,
    val patientIds: Set<Long>
)