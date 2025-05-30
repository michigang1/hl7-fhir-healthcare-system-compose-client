import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import data.AuthApiService
import data.model.JwtResponse
import data.remote.RetrofitClient
import presentation.*
import utils.TokenManager

// 1. Определяем экраны, доступные после входа в систему
enum class AppScreen(val title: String) {
    DASHBOARD("Dashboard"),
    PATIENTS("Patients"), // Пример дополнительного экрана
    PROFILE("Profile")    // Еще один пример
}

// 2. Обновляем Sealed Class для навигации
sealed class Screen {
    object Login : Screen()
    object Register : Screen()
    // Храним JWT и текущий активный экран внутри основного приложения
    data class MainApplication(
        val jwtResponse: JwtResponse,
        val currentAppScreen: AppScreen = AppScreen.DASHBOARD // Экран по умолчанию после входа
    ) : Screen()
}

@Composable
fun AppNavigation(
    currentScreen: Screen,
    authApiService: AuthApiService,
    onNavigate: (Screen) -> Unit
) {
    when (currentScreen) {
        is Screen.Login -> {
            val loginViewModel = remember {
                LoginViewModel(
                    apiService = authApiService,
                    onLoginSuccess = { jwt ->
                        // При успешном входе переходим на MainApplication с экраном по умолчанию (DASHBOARD)
                        onNavigate(Screen.MainApplication(jwt, AppScreen.DASHBOARD))
                    }
                )
            }
            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToRegister = { onNavigate(Screen.Register) }
            )
        }

        is Screen.Register -> {
            val signUpViewModel = remember {
                SignUpViewModel(
                    apiService = authApiService,
                    onSignUpSuccess = { signUpResponse ->
                        println("Регистрация успешна: ${signUpResponse.message}")
                        onNavigate(Screen.Login) // После регистрации на экран входа
                    }
                )
            }
            SignUpScreen(
                viewModel = signUpViewModel,
                onNavigateToLogin = { onNavigate(Screen.Login) },
                onSignUpSuccessNavigation = {
                    println("SignUpScreen: onSignUpSuccessNavigation -> переход на Login")
                    onNavigate(Screen.Login)
                }
            )
        }

        is Screen.MainApplication -> {
            // Пользователь вошел в систему, показываем основной интерфейс с Scaffold и TopAppBar
            MainApplicationScaffold(
                jwtResponse = currentScreen.jwtResponse,
                initialAppScreen = currentScreen.currentAppScreen,
                onNavigateToAppScreen = { newAppScreen ->
                    // Обновляем состояние MainApplication новым выбранным AppScreen
                    onNavigate(Screen.MainApplication(currentScreen.jwtResponse, newAppScreen))
                },
                onLogout = {
                    onNavigate(Screen.Login) // При выходе возвращаемся на экран входа
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApplicationScaffold(
    jwtResponse: JwtResponse,
    initialAppScreen: AppScreen,
    onNavigateToAppScreen: (AppScreen) -> Unit,
    onLogout: () -> Unit
) {
    var currentActiveAppScreen by remember { mutableStateOf(initialAppScreen) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Healthcare App - ${currentActiveAppScreen.title}") }, // Динамический заголовок
                actions = {
                    // Кнопки навигации для каждого AppScreen
                    AppScreen.values().forEach { appScreen ->
                        NavButton(
                            label = appScreen.title,
                            screen = appScreen,
                            current = currentActiveAppScreen,
                            onClick = { selectedScreen ->
                                currentActiveAppScreen = selectedScreen
                                onNavigateToAppScreen(selectedScreen)
                            }
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    OutlinedButton(onClick = {
                        TokenManager.clearToken()
                        onLogout
                    }) { Text("Logout") }
                }
            )
        },

        bottomBar = {
            BottomAppBar {
                 Text(
                     "Logged-in token: ${jwtResponse.token.take(10)}...", // Пример использования JWT
                    modifier = Modifier.padding(8.dp)
                )
            }
         }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            // Отображаем контент в зависимости от выбранного AppScreen
            when (currentActiveAppScreen) {
                AppScreen.DASHBOARD -> DashboardScreen()
                AppScreen.PATIENTS -> PatientsScreen(PatientViewModel(RetrofitClient.patientApiService))
                AppScreen.PROFILE -> PlaceholderScreen("User Profile Screen")
                // Добавьте другие экраны здесь
            }
        }
    }
}

@Composable
private fun NavButton(
    label: String,
    screen: AppScreen,
    current: AppScreen,
    onClick: (AppScreen) -> Unit
) {
    TextButton(
        onClick = { onClick(screen) },
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (current == screen) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant // или onPrimaryContainer / secondary для лучшего контраста
        )
    ) { Text(label) }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$name – coming soon…", style = MaterialTheme.typography.headlineSmall)
    }
}

fun main() = application {
    // Начальный экран - Login
    var currentScreenState by remember { mutableStateOf<Screen>(Screen.Login) }
    val authApiService = RetrofitClient.authApiService

    Window(
        onCloseRequest = ::exitApplication,
        title = "Healthcare Client"
    ) {
        MaterialTheme { // Убедитесь, что MaterialTheme обертывает ваше приложение
            AppNavigation(
                currentScreen = currentScreenState,
                authApiService = authApiService,
                onNavigate = { newScreen -> currentScreenState = newScreen }
            )
        }
    }
}