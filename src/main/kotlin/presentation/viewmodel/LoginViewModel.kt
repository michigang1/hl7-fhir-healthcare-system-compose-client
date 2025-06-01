package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.remote.services.AuthApiService
import data.model.JwtResponse
import data.model.LoginRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import presentation.state.LoginState
import utils.TokenManager

/**
 * ViewModel for the login screen.
 */
class LoginViewModel(
    private val apiService: AuthApiService,
    private val onLoginSuccess: (JwtResponse) -> Unit,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    // Input fields
    var username by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    // UI state
    var state by mutableStateOf(LoginState())
        private set

    private val viewModelScope = CoroutineScope(mainDispatcher + SupervisorJob())

    fun onUsernameChange(newUsername: String) {
        username = newUsername
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
    }

    fun login() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val retrofitResponse = withContext(ioDispatcher) {
                    apiService.login(LoginRequest(username, password))
                }

                if (retrofitResponse.isSuccessful && retrofitResponse.body() != null) {
                    val jwtResponse = retrofitResponse.body()!!
                    TokenManager.setToken(jwtResponse.token) // Save token
                    state = state.copy(isLoading = false, jwtResponse = jwtResponse)
                    onLoginSuccess(jwtResponse)
                } else {
                    TokenManager.clearToken() // Clear token in case of error
                    state = state.copy(
                        isLoading = false,
                        errorMessage = "Login failed: ${retrofitResponse.message() ?: "Server error ${retrofitResponse.code()}"}"
                    )
                }
            } catch (e: Exception) {
                TokenManager.clearToken() // Clear token in case of error
                state = state.copy(
                    isLoading = false,
                    errorMessage = "Login failed: ${e.message ?: "Unknown error"}"
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
