package presentation // Или ваша папка для экранов, например, screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import data.model.CareTask
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import data.model.EventData


@Composable
fun DashboardScreen() = Row(Modifier.fillMaxSize().padding(16.dp)) {
    EventsSection(
        modifier = Modifier.weight(0.33f).fillMaxHeight()
    )
    Spacer(Modifier.width(16.dp))
    TodayToDoSection(
        modifier = Modifier.weight(0.42f).fillMaxHeight()
    )
    Spacer(Modifier.width(16.dp))
    WarningsSection(
        modifier = Modifier.weight(0.25f).fillMaxHeight()
    )
}

@Composable
private fun EventCard(event: EventData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                event.theme, // Отображаем тему
                style = MaterialTheme.typography.titleMedium, // Используем более крупный стиль для темы
                fontWeight = FontWeight.Bold // Делаем тему жирной
            )
            Spacer(Modifier.height(4.dp))
            Text(event.text, style = MaterialTheme.typography.bodyMedium) // Текст события чуть меньше темы
            Spacer(Modifier.height(8.dp)) // Небольшой отступ перед деталями
            Text(
                "Patient(s): ${event.patients.joinToString()}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Author: ${event.author}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Time: ${event.time}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EventsSection(modifier: Modifier = Modifier) {
    // TODO: Заменить на реальные данные или ViewModel
    val sampleEvents = remember {
        listOf(
            EventData(
                1,
                "Routine Check",
                "Regular check-up performed.",
                listOf("John Doe", "Jane Smith"),
                "Dr. Eva Brown",
                "09:30"
            ),
            EventData(
                2,
                "Medication",
                "Medication administered as per schedule.",
                listOf("Robert Williams"),
                "Nurse Mike Green",
                "10:15"
            ),
            EventData(
                3,
                "Urgent Response",
                "Emergency call response and initial assessment.",
                listOf("Lisa Ray"),
                "Paramedic Team A",
                "11:00"
            ),
            EventData(
                4,
                "Consultation",
                "Scheduled consultation regarding new symptoms.",
                listOf("Anna Bell"),
                "Dr. Smith",
                "14:00"
            )
        )
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp).fillMaxHeight()) {
            Text("Events", style = MaterialTheme.typography.titleMedium)
            Divider(Modifier.padding(vertical = 8.dp))

            if (sampleEvents.isEmpty()) {
                Text("No new events.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sampleEvents) { event ->
                        EventCard(event = event)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayToDoSection(modifier: Modifier = Modifier) {
    // TODO: Заменить на реальные данные или ViewModel
    val initialTasks = remember {
        listOf(
            CareTask(1, "Medication AM – John Doe", "08:00"),
            CareTask(2, "Breakfast – Ward B", "09:00"),
            CareTask(3, "Pressure-ulcer check – Ann M.", "10:30"),
            CareTask(4, "Physiotherapy – Mr. Smith", "11:00")
        )
    }
    val tasks = remember { mutableStateListOf(*initialTasks.toTypedArray()) }
    val completedTasksCount = tasks.count { it.isCompleted }
    val totalTasksCount = tasks.size
    val overallProgress = if (totalTasksCount > 0) {
        completedTasksCount.toFloat() / totalTasksCount.toFloat()
    } else {
        0f
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Today to Do", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { overallProgress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
            Divider(Modifier.padding(vertical = 8.dp))
            if (tasks.isEmpty()) {
                Text("No tasks for today.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn {
                    items(tasks.size) { index ->
                        val task = tasks[index]
                        CareTaskItem(
                            task = task,
                            onCompletedChange = { newCompletedState ->
                                tasks[index] = task.copy(isCompleted = newCompletedState)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CareTaskItem(task: CareTask, onCompletedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onCompletedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = task.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (task.isCompleted) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Task completed",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun WarningsSection(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Warnings", style = MaterialTheme.typography.titleMedium)
            Divider(Modifier.padding(vertical = 8.dp))
            // TODO: Заменить на реальные данные или ViewModel
            WarningRow("Expired medication", "Müller K.", "-2 d")
            WarningRow("Stool protocol late", "Adler W.", "-1 d")
            WarningRow("Care plan overdue", "Rinker H.", "-3 d")
            WarningRow("Low supplies: Bandages", "Station 1", "Critical")
        }
    }
}

@Composable
private fun WarningRow(text: String, subject: String, detail: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            Text(subject, style = MaterialTheme.typography.bodySmall)
        }
        Text(detail, style = MaterialTheme.typography.bodySmall)
    }
}