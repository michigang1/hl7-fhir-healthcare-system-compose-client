package presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.AuthApiService
import data.model.ApiError // Предполагаем, что этот класс доступен
import data.model.SignUpRequest
import data.model.SignUpResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException // Для обработки HTTP ошибок от Retrofit

class SignUpViewModel(
    private val apiService: AuthApiService,
    private val onSignUpSuccess: (SignUpResponse) -> Unit, // Коллбэк при успехе
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    var username by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    // Список доступных ролей для выбора
    val availableRoles: List<String> = listOf("ROLE_NURSE", "ROLE_DOCTOR", "ROLE_SOCIAL_WORKER") // Пример ролей

    // Выбранная роль, инициализируется первой из списка доступных ролей
    var selectedRole by mutableStateOf(availableRoles.firstOrNull() ?: "")

    private val _signUpState = MutableStateFlow<SignUpScreenState>(SignUpScreenState.Idle)
    val signUpState: StateFlow<SignUpScreenState> = _signUpState.asStateFlow()

    private val viewModelScope = CoroutineScope(mainDispatcher + SupervisorJob())

    // JSON парсер для обработки ошибок API
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun onUsernameChange(newUsername: String) {
        username = newUsername
        _signUpState.value = SignUpScreenState.Idle
    }

    fun onEmailChange(newEmail: String) {
        email = newEmail
        _signUpState.value = SignUpScreenState.Idle
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
        _signUpState.value = SignUpScreenState.Idle
    }

    fun onConfirmPasswordChange(newConfirmPassword: String) {
        confirmPassword = newConfirmPassword
        _signUpState.value = SignUpScreenState.Idle
    }

    // Метод для обновления выбранной роли
    fun onRoleChange(newRole: String) {
        selectedRole = newRole
        _signUpState.value = SignUpScreenState.Idle // Сброс состояния, если необходимо
    }

    fun signUp() {
        if (username.isBlank()) {
            _signUpState.value = SignUpScreenState.Error("Имя пользователя не может быть пустым.")
            return
        }
        if (email.isBlank()) { // Простая проверка email
            _signUpState.value = SignUpScreenState.Error("Введите корректный email.")
            return
        }
        if (password.isBlank()) {
            _signUpState.value = SignUpScreenState.Error("Пароль не может быть пустым.")
            return
        }
        if (password.length < 6) { // Пример минимальной длины пароля
            _signUpState.value = SignUpScreenState.Error("Пароль должен содержать не менее 6 символов.")
            return
        }
        if (password != confirmPassword) {
            _signUpState.value = SignUpScreenState.Error("Пароли не совпадают.")
            return
        }
        if (selectedRole.isBlank()) {
            _signUpState.value = SignUpScreenState.Error("Выберите роль.")
            return
        }

        viewModelScope.launch {
            _signUpState.value = SignUpScreenState.Loading
            try {
                val request = SignUpRequest(
                    username = username,
                    email = email,
                    password = password,
                    roles = setOf(selectedRole) // Передаем выбранную роль как Set
                )
                val response = withContext(ioDispatcher) {
                    apiService.register(request)
                }
                _signUpState.value = SignUpScreenState.Success(response)
                onSignUpSuccess(response)
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorMessage = if (errorBody != null) {
                    try {
                        val apiError = json.decodeFromString<ApiError>(errorBody)
                        "${apiError.error}: ${apiError.description} (Статус: ${apiError.status})"
                    } catch (jsonException: Exception) {
                        "Ошибка сервера: ${e.code()} ${e.message()}. Тело ответа: $errorBody"
                    }
                } else {
                    "Ошибка сети или сервера: ${e.message()}"
                }
                _signUpState.value = SignUpScreenState.Error(errorMessage)
            } catch (e: Exception) {
                _signUpState.value = SignUpScreenState.Error("Регистрация не удалась: ${e.message ?: "Неизвестная ошибка"}")
            }
        }
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}

// Состояния для экрана регистрации
sealed class SignUpScreenState {
    object Idle : SignUpScreenState()
    object Loading : SignUpScreenState()
    data class Success(val response: SignUpResponse) : SignUpScreenState()
    data class Error(val message: String) : SignUpScreenState()
}