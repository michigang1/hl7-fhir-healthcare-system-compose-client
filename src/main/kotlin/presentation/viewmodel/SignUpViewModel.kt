package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.remote.services.AuthApiService
import data.model.ApiError
import data.model.SignUpRequest
import data.model.SignUpResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import presentation.state.SignUpState
import retrofit2.HttpException

/**
 * ViewModel for the sign-up screen.
 */
class SignUpViewModel(
    private val apiService: AuthApiService,
    private val onSignUpSuccess: (SignUpResponse) -> Unit,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    // Input fields
    var username by mutableStateOf("")
        private set

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var confirmPassword by mutableStateOf("")
        private set

    // List of available roles for selection
    val availableRoles: List<String> = listOf("ROLE_NURSE", "ROLE_DOCTOR", "ROLE_SOCIAL_WORKER")

    // Selected role, initialized with the first available role
    var selectedRole by mutableStateOf(availableRoles.firstOrNull() ?: "")
        private set

    // UI state
    var state by mutableStateOf(SignUpState())
        private set

    private val viewModelScope = CoroutineScope(mainDispatcher + SupervisorJob())

    // JSON parser for API error handling
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun onUsernameChange(newUsername: String) {
        username = newUsername
    }

    fun onEmailChange(newEmail: String) {
        email = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
    }

    fun onConfirmPasswordChange(newConfirmPassword: String) {
        confirmPassword = newConfirmPassword
    }

    fun onRoleChange(newRole: String) {
        selectedRole = newRole
    }

    fun signUp() {
        // Input validation
        if (username.isBlank()) {
            state = state.copy(errorMessage = "Username cannot be empty.")
            return
        }
        if (email.isBlank()) {
            state = state.copy(errorMessage = "Please enter a valid email.")
            return
        }
        if (password.isBlank()) {
            state = state.copy(errorMessage = "Password cannot be empty.")
            return
        }
        if (password.length < 6) {
            state = state.copy(errorMessage = "Password must be at least 6 characters long.")
            return
        }
        if (password != confirmPassword) {
            state = state.copy(errorMessage = "Passwords do not match.")
            return
        }
        if (selectedRole.isBlank()) {
            state = state.copy(errorMessage = "Please select a role.")
            return
        }

        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val request = SignUpRequest(
                    username = username,
                    email = email,
                    password = password,
                    roles = setOf(selectedRole)
                )
                val retrofitResponse = withContext(ioDispatcher) {
                    apiService.register(request)
                }

                if (retrofitResponse.isSuccessful && retrofitResponse.body() != null) {
                    val signUpResponse = retrofitResponse.body()!!
                    state = state.copy(isLoading = false, response = signUpResponse)
                    onSignUpSuccess(signUpResponse)
                } else {
                    val errorMessage = "Registration failed: ${retrofitResponse.message() ?: "Server error ${retrofitResponse.code()}"}"
                    state = state.copy(isLoading = false, errorMessage = errorMessage)
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorMessage = if (errorBody != null) {
                    try {
                        val apiError = json.decodeFromString<ApiError>(errorBody)
                        "${apiError.error}: ${apiError.description} (Status: ${apiError.status})"
                    } catch (jsonException: Exception) {
                        "Server error: ${e.code()} ${e.message()}. Response body: $errorBody"
                    }
                } else {
                    "Network or server error: ${e.message()}"
                }
                state = state.copy(isLoading = false, errorMessage = errorMessage)
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    errorMessage = "Registration failed: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    fun clearError() {
        state = state.copy(errorMessage = null)
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}
