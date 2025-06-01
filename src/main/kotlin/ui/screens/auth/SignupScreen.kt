package ui.screens.auth

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
import presentation.viewmodel.SignUpViewModel

@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel, // Убедитесь, что SignUpViewModel обновлен соответствующим образом
    onNavigateToLogin: () -> Unit,
    onSignUpSuccessNavigation: () -> Unit
) {
    val signUpState = viewModel.state

    // В идеале, 'roles' должны приходить из viewModel.availableRoles
    // Для примера, если viewModel еще не обновлен, можно использовать временный список:
    val roles = viewModel.availableRoles // или listOf("NURSE", "DOCTOR", "SOCIAL_WORKER")

    LaunchedEffect(signUpState.response) {
        if (signUpState.response != null) {
            // Navigation after successful registration
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
            isError = signUpState.errorMessage?.contains("Username", ignoreCase = true) == true
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.onEmailChange(it) },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            isError = signUpState.errorMessage?.contains("email", ignoreCase = true) == true
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
            isError = signUpState.errorMessage?.contains("Password", ignoreCase = true) == true
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
            isError = signUpState.errorMessage?.contains("Passwords do not match", ignoreCase = true) == true
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
            enabled = !signUpState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (signUpState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Sign Up")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("Already have an account? Login")
        }

        // Display error or success messages
        signUpState.errorMessage?.let { errorMessage ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        signUpState.response?.let { response ->
            // Success message (can be removed if there's navigation and/or Snackbar)
            Spacer(modifier = Modifier.height(8.dp))
            Text(response.message, color = MaterialTheme.colorScheme.primary)
        }
    }
}
