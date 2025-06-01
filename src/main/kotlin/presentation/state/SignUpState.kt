package presentation.state

import data.model.SignUpResponse

/**
 * Represents the state of the sign-up screen.
 */
data class SignUpState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val response: SignUpResponse? = null
)