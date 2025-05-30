package presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel, // Убедитесь, что SignUpViewModel обновлен соответствующим образом
    onNavigateToLogin: () -> Unit,
    onSignUpSuccessNavigation: () -> Unit
) {
    val signUpState by viewModel.signUpState.collectAsState()

    // В идеале, 'roles' должны приходить из viewModel.availableRoles
    // Для примера, если viewModel еще не обновлен, можно использовать временный список:
    val roles = viewModel.availableRoles // или listOf("NURSE", "DOCTOR", "SOCIAL_WORKER")

    LaunchedEffect(signUpState) {
        if (signUpState is SignUpScreenState.Success) {
            // Навигация после успешной регистрации
            onSignUpSuccessNavigation()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create Account", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = viewModel.username,
            onValueChange = { viewModel.onUsernameChange(it) },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = signUpState is SignUpScreenState.Error && (signUpState as SignUpScreenState.Error).message.contains("Имя пользователя", ignoreCase = true)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.onEmailChange(it) },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            isError = signUpState is SignUpScreenState.Error && (signUpState as SignUpScreenState.Error).message.contains("email", ignoreCase = true)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.onPasswordChange(it) },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            isError = signUpState is SignUpScreenState.Error && (signUpState as SignUpScreenState.Error).message.contains("Пароль", ignoreCase = true)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.confirmPassword,
            onValueChange = { viewModel.onConfirmPasswordChange(it) },
            label = { Text("Confirm Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            isError = signUpState is SignUpScreenState.Error && (signUpState as SignUpScreenState.Error).message.contains("Пароли не совпадают", ignoreCase = true)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Column(Modifier.selectableGroup()) { // Группа для радиокнопок
            roles.forEach { roleText ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (viewModel.selectedRole == roleText),
                            onClick = { viewModel.onRoleChange(roleText) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (viewModel.selectedRole == roleText),
                        onClick = null // null, так как обработчик на Row
                    )
                    Text(
                        text = roleText.replace("_", " ").lowercase()
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.signUp() },
            enabled = signUpState !is SignUpScreenState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (signUpState is SignUpScreenState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Sign Up")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("Already have an account? Login")
        }

        // Отображение сообщений об ошибках или успехе
        when (val state = signUpState) {
            is SignUpScreenState.Error -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            is SignUpScreenState.Success -> {
                // Сообщение об успехе (можно убрать, если есть навигация и/или Snackbar)
                Spacer(modifier = Modifier.height(8.dp))
                Text(state.response.message, color = MaterialTheme.colorScheme.primary)
            }
            else -> {} // Idle, Loading
        }
    }
}