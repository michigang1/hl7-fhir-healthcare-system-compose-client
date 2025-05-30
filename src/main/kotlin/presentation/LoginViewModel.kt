package presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.AuthApiService
import data.model.JwtResponse
import data.model.LoginRequest // Убедитесь, что этот класс доступен
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel // Для отмены scope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import utils.TokenManager

class LoginViewModel(
    private val apiService: AuthApiService,
    private val onLoginSuccess: (JwtResponse) -> Unit,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    var username by mutableStateOf("")
    var password by mutableStateOf("")

    private val _loginState = MutableStateFlow<LoginScreenState>(LoginScreenState.Idle)
    val loginState: StateFlow<LoginScreenState> = _loginState.asStateFlow()

    private val viewModelScope = CoroutineScope(mainDispatcher + SupervisorJob())

    fun onUsernameChange(newUsername: String) {
        username = newUsername
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
    }

    fun login() {
        viewModelScope.launch {
            _loginState.value = LoginScreenState.Loading
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.login(LoginRequest(username, password))
                }
                TokenManager.setToken(response.token) // Сохраняем токен
                _loginState.value = LoginScreenState.Success(response)
                onLoginSuccess(response)
            } catch (e: Exception) {
                TokenManager.clearToken() // Очищаем токен в случае ошибки
                _loginState.value = LoginScreenState.Error("Login failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}

sealed class LoginScreenState {
    object Idle : LoginScreenState()
    object Loading : LoginScreenState()
    data class Success(val jwtResponse: JwtResponse) : LoginScreenState()
    data class Error(val message: String) : LoginScreenState()
}