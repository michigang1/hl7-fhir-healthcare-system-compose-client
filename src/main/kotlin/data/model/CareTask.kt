package data.model

data class CareTask(
    val id: Int,
    val description: String,
    val time: String,
    var isCompleted: Boolean = false
)

