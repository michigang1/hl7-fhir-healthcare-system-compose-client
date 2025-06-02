package data.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import data.serializers.LocalDateTimeSerializer

@Serializable
data class MeasureDto(
    val id: Long? = null,
    val goalId: Long,
    val name: String,
    val description: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val scheduledDateTime: LocalDateTime,
    val isCompleted: Boolean = false
)
