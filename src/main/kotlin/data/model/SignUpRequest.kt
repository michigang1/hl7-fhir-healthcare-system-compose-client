package data.model

import kotlinx.serialization.Serializable

@Serializable
data class SignUpRequest (
    val username:String,
    val email: String,
    val password: String,
    val roles: Set<String>
)
