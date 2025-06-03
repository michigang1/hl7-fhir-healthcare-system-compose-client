package data.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import data.serializers.LocalDateTimeSerializer

@Serializable
data class EventResponse(
    val id: Long,
    val name: String,
    val description: String,
    val authorId: Long,
    val authorUsername: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val eventDateTime: LocalDateTime,
    val patients: List<PatientResponse>
)