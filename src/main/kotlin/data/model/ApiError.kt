package data.model

data class ApiError(
    val status: Int,
    val error: String,
    val description: String
)