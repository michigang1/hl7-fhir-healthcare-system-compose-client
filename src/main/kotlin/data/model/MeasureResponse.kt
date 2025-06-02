package data.model

import kotlinx.serialization.Serializable

/**
 * Response model for a Measure.
 */
@Serializable
data class MeasureResponse(
    val id: Long,
    val name: String,
    val description: String,
    val templateId: Long? = null,
    val templateName: String? = null
)