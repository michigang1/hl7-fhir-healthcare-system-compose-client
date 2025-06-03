package ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import data.model.CareTask
import data.model.EventDto
import data.model.MeasureDto
import data.model.PatientDto
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import data.model.EventData
import presentation.viewmodel.CarePlanViewModel
import presentation.viewmodel.EventViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter


@Composable
fun DashboardScreen(carePlanViewModel: CarePlanViewModel, eventViewModel: EventViewModel) {
    // Load all patients with care plans when the screen is first displayed
    LaunchedEffect(Unit) {
        carePlanViewModel.loadPatientsWithCarePlans()
        eventViewModel.loadAllEvents()
    }

    Row(Modifier.fillMaxSize().padding(16.dp)) {
        EventsSection(
            modifier = Modifier.weight(0.33f).fillMaxHeight(),
            eventViewModel = eventViewModel
        )
        Spacer(Modifier.width(16.dp))
        TodayToDoSection(
            modifier = Modifier.weight(0.42f).fillMaxHeight(),
            carePlanViewModel = carePlanViewModel
        )
        Spacer(Modifier.width(16.dp))
        WarningsSection(
            modifier = Modifier.weight(0.25f).fillMaxHeight(),
            carePlanViewModel = carePlanViewModel
        )
    }

    // Show event add/edit dialog if needed
    val eventState = eventViewModel.state
    if (eventState.showAddOrEditDialog) {
        eventState.draftEvent?.let { event ->
            EventDialog(
                event = event,
                isEditing = eventState.isEditing,
                patients = carePlanViewModel.state.patientsWithCarePlans,
                onDismiss = { eventViewModel.closeAddOrEditEventDialog() },
                onSave = { eventViewModel.saveEvent() },
                onUpdate = { updatedEvent -> 
                    eventViewModel.updateDraftEvent { _ -> updatedEvent }
                }
            )
        }
    }

    // Show error message if needed
    eventState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { eventViewModel.clearErrorMessage() },
            title = { Text("Error") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { eventViewModel.clearErrorMessage() }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun DatePickerWheel(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    // Extract year, month, day from the selected date
    var year by remember { mutableStateOf(selectedDate.year) }
    var month by remember { mutableStateOf(selectedDate.monthValue) }
    var day by remember { mutableStateOf(selectedDate.dayOfMonth) }

    // Update the selected date when any component changes
    LaunchedEffect(year, month, day) {
        try {
            // Adjust day if it exceeds the maximum for the selected month
            val maxDay = LocalDate.of(year, month, 1).lengthOfMonth()
            val adjustedDay = if (day > maxDay) maxDay else day

            val newDate = LocalDate.of(year, month, adjustedDay)
            onDateSelected(newDate)
        } catch (e: Exception) {
            // Invalid date, don't update
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Year picker
        NumberPickerWheel(
            value = year,
            onValueChange = { year = it },
            range = 2020..2030,
            modifier = Modifier.weight(1f)
        )

        // Month picker
        NumberPickerWheel(
            value = month,
            onValueChange = { month = it },
            range = 1..12,
            modifier = Modifier.weight(1f)
        )

        // Day picker
        NumberPickerWheel(
            value = day,
            onValueChange = { day = it },
            range = 1..31, // We'll adjust this if needed in LaunchedEffect
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimePickerWheel(
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    // Extract hour and minute from the selected time
    var hour by remember { mutableStateOf(selectedTime.hour) }
    var minute by remember { mutableStateOf(selectedTime.minute) }

    // Update the selected time when any component changes
    LaunchedEffect(hour, minute) {
        try {
            val newTime = LocalTime.of(hour, minute)
            onTimeSelected(newTime)
        } catch (e: Exception) {
            // Invalid time, don't update
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hour picker
        NumberPickerWheel(
            value = hour,
            onValueChange = { hour = it },
            range = 0..23,
            modifier = Modifier.weight(1f)
        )

        Text(":", style = MaterialTheme.typography.headlineMedium)

        // Minute picker
        NumberPickerWheel(
            value = minute,
            onValueChange = { minute = it },
            range = 0..59,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NumberPickerWheel(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier
) {
    val items = range.toList()
    val state = rememberScrollState()

    // Calculate the visible items
    val visibleItems = 5 // Number of items visible at once
    val itemHeight = 30.dp

    Column(
        modifier = modifier
            .verticalScroll(state),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items.forEach { item ->
            val isSelected = item == value

            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth()
                    .clickable { onValueChange(item) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.toString().padStart(2, '0'),
                    style = if (isSelected) 
                        MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold) 
                    else 
                        MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
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
private fun EventsSection(modifier: Modifier = Modifier, eventViewModel: EventViewModel) {
    // Get events from the ViewModel
    val state = eventViewModel.state

    // Refresh data when state changes
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            eventViewModel.loadAllEvents()
        }
    }

    // Convert EventDto to EventData for UI display
    val events = state.events.map { it.toEventData() }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp).fillMaxHeight()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Events", style = MaterialTheme.typography.titleMedium)
                IconButton(
                    onClick = { eventViewModel.openAddEventDialog(1L) }, // Using a default patient ID of 1
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Event",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Divider(Modifier.padding(vertical = 8.dp))

            if (events.isEmpty()) {
                Text("No new events.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events) { event ->
                        EventCard(event = event)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayToDoSection(modifier: Modifier = Modifier, carePlanViewModel: CarePlanViewModel) {
    // Get today's date
    val today = LocalDate.now()

    // Get all measures from all patients' care plans
    val allMeasures = mutableSetOf<MeasureDto>() // Using a Set to prevent duplicates
    val state = carePlanViewModel.state

    // Refresh data when state changes
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            // Reload data for all patients
            state.patientsWithCarePlans.forEach { patient ->
                carePlanViewModel.loadCarePlansForPatient(patient.id)
            }
        }
    }

    // Collect measures for all patients
    state.patientsWithCarePlans.forEach { patient ->
        // For each patient, load their care plans if not already loaded
        LaunchedEffect(patient.id) {
            carePlanViewModel.loadCarePlansForPatient(patient.id)
        }

        // Get all measures from all goals for this patient
        state.measuresForGoals.values.forEach { measures ->
            allMeasures.addAll(measures)
        }
    }

    // Filter measures for today and sort by time
    val todayMeasures = allMeasures.filter { measure ->
        val measureDate = measure.scheduledDateTime.toLocalDate()
        measureDate == today
    }.sortedBy { it.scheduledDateTime }

    // Create a map of task ID to measure for updating
    val taskMeasureMap = remember { mutableMapOf<Int, MeasureDto>() }

    // Convert MeasureDto to CareTask
    val tasks = remember { 
        mutableStateListOf<CareTask>().apply {
            clear()
            addAll(todayMeasures.map { measure ->
                val taskId = measure.id?.toInt() ?: 0
                taskMeasureMap[taskId] = measure

                // Find the patient for this measure
                val patient = state.patientsWithCarePlans.find { patient ->
                    state.goalsForPatient.any { goal ->
                        goal.id == measure.goalId && goal.patientId == patient.id
                    }
                }

                val patientName = patient?.let { "${it.name} ${it.surname}" } ?: "Unknown"
                val roomNumber = patient?.roomNo ?: "N/A"

                CareTask(
                    id = taskId,
                    description = "${measure.name} - ${measure.description}",
                    time = measure.scheduledDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    isCompleted = measure.isCompleted,
                    patientName = patientName,
                    roomNumber = roomNumber
                )
            })
        }
    }

    // Update tasks when measures change
    LaunchedEffect(todayMeasures) {
        taskMeasureMap.clear()
        tasks.clear()
        tasks.addAll(todayMeasures.map { measure ->
            val taskId = measure.id?.toInt() ?: 0
            taskMeasureMap[taskId] = measure

            // Find the patient for this measure
            val patient = state.patientsWithCarePlans.find { patient ->
                state.goalsForPatient.any { goal ->
                    goal.id == measure.goalId && goal.patientId == patient.id
                }
            }

            val patientName = patient?.let { "${it.name} ${it.surname}" } ?: "Unknown"
            val roomNumber = patient?.roomNo ?: "N/A"

            CareTask(
                id = taskId,
                description = "${measure.name} - ${measure.description}",
                time = measure.scheduledDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                isCompleted = measure.isCompleted,
                patientName = patientName,
                roomNumber = roomNumber
            )
        })
    }

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
                                // Update the local task state
                                tasks[index] = task.copy(isCompleted = newCompletedState)

                                // Get the corresponding measure and update it in the backend
                                val measure = taskMeasureMap[task.id]
                                if (measure != null && measure.id != null) {
                                    // Create updated measure
                                    val updatedMeasure = measure.copy(isCompleted = newCompletedState)

                                    // Update the measure in the backend
                                    carePlanViewModel.updateMeasure(
                                        goalId = measure.goalId,
                                        measureId = measure.id,
                                        measureDto = updatedMeasure
                                    )
                                }
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
                    text = "Patient: ${task.patientName} - Room: ${task.roomNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
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
private fun WarningsSection(modifier: Modifier = Modifier, carePlanViewModel: CarePlanViewModel) {
    // Get current date and time
    val now = LocalDateTime.now()
    val today = LocalDate.now()

    // Get all measures from all patients' care plans
    val allMeasures = mutableSetOf<MeasureDto>() // Using a Set to prevent duplicates
    val state = carePlanViewModel.state

    // Refresh data when state changes
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            // Reload data for all patients
            state.patientsWithCarePlans.forEach { patient ->
                carePlanViewModel.loadCarePlansForPatient(patient.id)
            }
        }
    }

    // Collect measures for all patients
    state.patientsWithCarePlans.forEach { patient ->
        // Get all measures from all goals for this patient
        state.measuresForGoals.values.forEach { measures ->
            allMeasures.addAll(measures)
        }
    }

    // Filter for overdue measures (scheduled before today and not completed)
    val overdueMeasures = allMeasures.filter { measure ->
        !measure.isCompleted && measure.scheduledDateTime.toLocalDate().isBefore(today)
    }.sortedBy { it.scheduledDateTime }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Warnings", style = MaterialTheme.typography.titleMedium)
            Divider(Modifier.padding(vertical = 8.dp))

            if (overdueMeasures.isEmpty()) {
                Text("No warnings", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn {
                    items(overdueMeasures) { measure ->
                        val daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                            measure.scheduledDateTime.toLocalDate(), 
                            today
                        )
                        val overdueText = when {
                            daysOverdue == 0L -> "Today"
                            daysOverdue == 1L -> "-1 d"
                            else -> "-$daysOverdue d"
                        }

                        // Find the patient for this measure
                        val patientName = state.patientsWithCarePlans.find { patient ->
                            state.goalsForPatient.any { goal ->
                                goal.id == measure.goalId && goal.patientId == patient.id
                            }
                        }?.let { "${it.name} ${it.surname}" } ?: "Unknown"

                        WarningRow(
                            text = measure.name,
                            subject = patientName,
                            detail = overdueText
                        )
                    }
                }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDialog(
    event: EventDto,
    isEditing: Boolean,
    patients: List<PatientDto>,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onUpdate: (EventDto) -> Unit
) {
    var name by remember { mutableStateOf(event.name) }
    var description by remember { mutableStateOf(event.description) }

    // Only track time, date will always be today
    var selectedTime by remember { mutableStateOf(event.eventDateTime.toLocalTime()) }

    // For patient selection
    val selectedPatientIds = remember { mutableStateListOf<Long>() }

    // Initialize selected patients
    LaunchedEffect(event) {
        selectedPatientIds.clear()
        selectedPatientIds.addAll(event.patients.map { it.id })
    }

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("Please fill all required fields.") }

    // Update the ViewModel's draft event when any field changes
    LaunchedEffect(name, description, selectedTime, selectedPatientIds) {
        try {
            // Always use today's date
            val today = LocalDate.now()
            val combinedDateTime = LocalDateTime.of(today, selectedTime)
            val updatedEvent = event.copy(
                name = name,
                description = description,
                eventDateTime = combinedDateTime,
                patients = patients.filter { selectedPatientIds.contains(it.id) }
            )
            onUpdate(updatedEvent)
        } catch (e: Exception) {
            // Time error, don't update the draft
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (isEditing) "Edit Event" else "Add New Event", 
                    style = MaterialTheme.typography.headlineSmall
                )

                if (showError) {
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; showError = false },
                    label = { Text("Event Name*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && name.isBlank()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it; showError = false },
                    label = { Text("Description*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 3,
                    isError = showError && description.isBlank()
                )

                // Time Picker
                Text(
                    "Time*",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
                )
                TimePickerWheel(
                    selectedTime = selectedTime,
                    onTimeSelected = { selectedTime = it; showError = false },
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )

                Text(
                    "Select Patients*",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                )

                // Patient selection
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                        .padding(8.dp)
                ) {
                    items(patients) { patient ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedPatientIds.contains(patient.id),
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selectedPatientIds.add(patient.id)
                                    } else {
                                        selectedPatientIds.remove(patient.id)
                                    }
                                    showError = false
                                }
                            )
                            Text(
                                "${patient.name} ${patient.surname} (Room: ${patient.roomNo})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (name.isBlank() || description.isBlank() || selectedPatientIds.isEmpty()) {
                                showError = true
                                errorMessage = "Please fill all required fields and select at least one patient."
                            } else {
                                onSave()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
