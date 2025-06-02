package data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import data.serializers.LocalDateSerializer
import data.serializers.LocalDateTimeSerializer

@Serializable
data class CarePlanRequest(
    val patientId: Long,
    val title: String,
    val description: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val endDate: LocalDate,
    val goal: CarePlanGoalRequest,
    val measures: List<CarePlanMeasureRequest>
)

@Serializable
data class CarePlanGoalRequest(
    val name: String,
    val description: String,
    val frequency: String,
    val duration: String
)

@Serializable
data class CarePlanMeasureRequest(
    val name: String,
    val description: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val scheduledDateTime: LocalDateTime,
    val isCompleted: Boolean = false
)
