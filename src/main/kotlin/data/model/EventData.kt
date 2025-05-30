package data.model

data class EventData(
    val id: Int,
    val theme: String,
    val text: String,
    val patients: List<String>,
    val author: String,
    val time: String
)
