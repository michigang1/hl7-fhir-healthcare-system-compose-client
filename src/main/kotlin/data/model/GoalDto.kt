package data.model

import kotlinx.serialization.Serializable

@Serializable
data class GoalDto(
    val id: Long? = null,
    val patientId: Long,
    val name: String,
    val description: String,
    val frequency: String = "",
    val duration: String = ""
)
