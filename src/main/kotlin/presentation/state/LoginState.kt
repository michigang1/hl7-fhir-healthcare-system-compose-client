package presentation.state

import data.model.JwtResponse

/**
 * Represents the state of the login screen.
 */
data class LoginState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val jwtResponse: JwtResponse? = null
)