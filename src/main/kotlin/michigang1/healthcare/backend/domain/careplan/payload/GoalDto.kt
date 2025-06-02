package michigang1.healthcare.backend.domain.careplan.payload

data class GoalDto(
    val id: Long? = null,
    val patientId: Long,
    val name: String,
    val description: String,
    val frequency: String,
    val duration: String
)