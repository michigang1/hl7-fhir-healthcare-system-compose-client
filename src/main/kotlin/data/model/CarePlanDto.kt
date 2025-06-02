package data.model

import data.serializers.LocalDateSerializer
import data.serializers.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
data class CarePlanDto(
    val id: Long,
    val patientId: Long,
    val title: String,
    val description: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val endDate: LocalDate,
    val goal: CarePlanGoalDto,
    val measures: List<CarePlanMeasureDto>
)

@Serializable
data class CarePlanGoalDto(
    val id: Long,
    val name: String,
    val description: String,
    val frequency: String,
    val duration: String
)

@Serializable
data class CarePlanMeasureDto(
    val id: Long,
    val name: String,
    val description: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val scheduledDateTime: LocalDateTime,
    val isCompleted: Boolean
)
