package ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ui.navigation.AppScreen

/**
 * Navigation button used in the top app bar for switching between app screens.
 * 
 * @param label Text to display on the button
 * @param screen The AppScreen this button represents
 * @param current The currently active AppScreen
 * @param onClick Callback when the button is clicked
 */
@Composable
fun NavButton(
    label: String,
    screen: AppScreen,
    current: AppScreen,
    onClick: (AppScreen) -> Unit
) {
    TextButton(
        onClick = { onClick(screen) },
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (current == screen) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) { Text(label) }
}

/**
 * A placeholder screen to display when a feature is not yet implemented.
 * 
 * @param name The name of the screen to display
 */
@Composable
fun PlaceholderScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$name – coming soon…", style = MaterialTheme.typography.headlineSmall)
    }
}