package ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import data.model.CarePlanDto
import data.model.CarePlanMeasureDto
import data.model.CarePlanGoalDto
import data.model.GoalDto
import data.model.MeasureDto
import presentation.viewmodel.CarePlanViewModel
import presentation.viewmodel.PatientViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun CarePlanScreen(
    patientViewModel: PatientViewModel,
    carePlanViewModel: CarePlanViewModel,
    patientId: Long
) {
    val patientState = patientViewModel.state
    val carePlanState = carePlanViewModel.state

    // State for the selected date
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    // State for date range selection for adding measures
    var showDateRangeDialog by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(7)) }

    // State for selected patient
    var selectedPatientId by remember { mutableStateOf(patientId) }

    // State for the goal selection dialog
    var showGoalSelectionDialog by remember { mutableStateOf(false) }
    var selectedGoalForMeasure by remember { mutableStateOf<GoalDto?>(null) }

    LaunchedEffect(Unit) {
        patientViewModel.loadPatients()
        carePlanViewModel.loadPatientsWithCarePlans()
    }

    LaunchedEffect(selectedPatientId) {
        if (selectedPatientId > 0) {
            patientViewModel.selectPatient(selectedPatientId)
            carePlanViewModel.loadCarePlansForPatient(selectedPatientId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Care Plans",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Date selector row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date selector
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedDate = selectedDate.minusDays(1) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous Day")
                }

                Text(
                    text = selectedDate.format(dateFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(onClick = { selectedDate = selectedDate.plusDays(1) }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next Day")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(onClick = { showDateRangeDialog = true }) {
                    Text("Add for Period")
                }
            }

            // Add Goal and Add Measure buttons (only show if a patient is selected)
            if (selectedPatientId > 0) {
                Row {
                    Button(onClick = { carePlanViewModel.openAddGoalDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Goal")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Goal")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Only enable Add Measure button if there are goals available
                    val hasGoals = carePlanState.goalsForPatient.isNotEmpty()
                    Button(
                        onClick = { 
                            // If there's only one goal, use it directly
                            if (carePlanState.goalsForPatient.size == 1) {
                                carePlanState.goalsForPatient.firstOrNull()?.id?.let { goalId ->
                                    carePlanViewModel.openAddMeasureDialog(goalId)
                                }
                            } else {
                                // Show goal selection dialog
                                showGoalSelectionDialog = true
                            }
                        },
                        enabled = hasGoals
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Measure")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Measure")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Patients list and measures details
        Row(modifier = Modifier.fillMaxSize()) {
            // Patients list (left panel)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Patients",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (patientState.isLoading || carePlanState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (patientState.patientsList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No patients found")
                        }
                    } else {
                        // Display patients list
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(patientState.patientsList) { patient ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedPatientId = patient.id }
                                        .padding(vertical = 4.dp),
                                    color = if (patient.id == selectedPatientId) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.surface
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Text(
                                            text = "${patient.name} ${patient.surname}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Room: ${patient.roomNo}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Measures for selected day (right panel)
            Card(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header with patient info if selected
                    patientState.selectedPatient?.let { patient ->
                        Text(
                            text = "Measures for ${patient.name} ${patient.surname}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Date: ${selectedDate.format(dateFormatter)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    } ?: run {
                        Text(
                            text = "Measures for Selected Day",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Date: ${selectedDate.format(dateFormatter)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Select a patient to view their measures",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    if (selectedPatientId > 0) {
                        if (carePlanState.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            // Get all measures for the selected patient
                            val allMeasures = mutableListOf<Pair<GoalDto, MeasureDto>>()

                            // Debug: Print goals and measures
                            println("DEBUG: Found ${carePlanState.goalsForPatient.size} goals for patient $selectedPatientId")
                            println("DEBUG: MeasuresForGoals map has ${carePlanState.measuresForGoals.size} entries")

                            // Collect measures from all goals
                            carePlanState.goalsForPatient.forEach { goal ->
                                val goalId = goal.id ?: 0L
                                val measures = carePlanState.measuresForGoals[goalId] ?: emptyList()

                                println("DEBUG: Goal $goalId (${goal.name}) has ${measures.size} measures")

                                // Filter measures for the selected date
                                val measuresForSelectedDate = measures.filter { measure ->
                                    val matches = measure.scheduledDateTime.toLocalDate() == selectedDate
                                    if (!matches) {
                                        println("DEBUG: Measure ${measure.id} scheduled for ${measure.scheduledDateTime.toLocalDate()} doesn't match selected date $selectedDate")
                                    }
                                    matches
                                }

                                println("DEBUG: After filtering, goal $goalId has ${measuresForSelectedDate.size} measures for date $selectedDate")

                                // Add to the list with goal information
                                measuresForSelectedDate.forEach { measure ->
                                    allMeasures.add(Pair(goal, measure))
                                    println("DEBUG: Added measure ${measure.id} (${measure.name}) to allMeasures")
                                }
                            }

                            // Sort measures by time as required
                            val sortedMeasures = allMeasures.sortedWith(
                                compareBy({ it.second.scheduledDateTime })
                            )

                            println("DEBUG: After sorting, there are ${sortedMeasures.size} measures")
                            sortedMeasures.forEachIndexed { index, (goal, measure) ->
                                println("DEBUG: Sorted measure $index: ${goal.name} - ${measure.name} at ${measure.scheduledDateTime}")
                            }

                            if (sortedMeasures.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No measures scheduled for this date")
                                }
                            } else {
                                // Display measures in a table format
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // Table header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "Goal",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "Measure",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "Time",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(0.5f)
                                        )
                                        Text(
                                            text = "Status",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(0.5f)
                                        )
                                        Spacer(modifier = Modifier.width(48.dp)) // Space for actions
                                    }

                                    // Table content
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(sortedMeasures) { (goal, measure) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = goal.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = measure.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = measure.scheduledDateTime.format(
                                                        DateTimeFormatter.ofPattern("HH:mm")
                                                    ),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.weight(0.5f)
                                                )
                                                Row(
                                                    modifier = Modifier.weight(0.5f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = if (measure.isCompleted) 
                                                            Icons.Default.Done 
                                                        else 
                                                            Icons.Default.Close,
                                                        contentDescription = if (measure.isCompleted) 
                                                            "Completed" 
                                                        else 
                                                            "Not Completed",
                                                        tint = if (measure.isCompleted) 
                                                            Color.Green 
                                                        else 
                                                            Color.Red,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (measure.isCompleted) "Done" else "Pending",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }

                                                // Actions
                                                Row {
                                                    IconButton(
                                                        onClick = { 
                                                            carePlanViewModel.openEditMeasureDialog(
                                                                CarePlanMeasureDto(
                                                                    id = measure.id ?: 0L,
                                                                    name = measure.name,
                                                                    description = measure.description,
                                                                    scheduledDateTime = measure.scheduledDateTime,
                                                                    isCompleted = measure.isCompleted
                                                                ), 
                                                                goal.id ?: 0L
                                                            ) 
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Edit, 
                                                            contentDescription = "Edit Measure",
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { 
                                                            measure.id?.let { measureId ->
                                                                carePlanViewModel.deleteMeasure(
                                                                    goal.id ?: 0L, 
                                                                    measureId
                                                                ) 
                                                            }
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Delete, 
                                                            contentDescription = "Delete Measure",
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            HorizontalDivider()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Care Plan Dialog
    if (carePlanState.showAddOrEditDialog) {
        CarePlanDialog(
            carePlan = carePlanState.draftCarePlan!!,
            isEditing = carePlanState.isEditing,
            onDismiss = { carePlanViewModel.closeAddOrEditCarePlanDialog() },
            onSave = { carePlanViewModel.saveCarePlan() },
            onUpdate = { updatedCarePlan ->
                carePlanViewModel.updateDraftCarePlan { updatedCarePlan }
            }
        )
    }

    // Date Range Dialog
    if (showDateRangeDialog) {
        // Only show if a patient is selected
        if (selectedPatientId > 0) {
            // Find the first goal for this patient, or create a new one if none exists
            val firstGoal = carePlanState.goalsForPatient.firstOrNull()

            DateRangeDialog(
                startDate = startDate,
                endDate = endDate,
                onDismiss = { showDateRangeDialog = false },
                onConfirm = { newStartDate: LocalDate, newEndDate: LocalDate ->
                    if (firstGoal != null) {
                        // If a goal exists, create measures for each day in the range
                        val goalId = firstGoal.id ?: 0L

                        // Create a measure for each day in the range
                        var currentDate = newStartDate
                        while (!currentDate.isAfter(newEndDate)) {
                            val measure = MeasureDto(
                                id = null,
                                goalId = goalId,
                                name = "Measure for ${currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}",
                                description = "Generated measure for ${currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}",
                                scheduledDateTime = LocalDateTime.of(currentDate, LocalDateTime.now().toLocalTime()),
                                isCompleted = false
                            )

                            // Create the measure
                            carePlanViewModel.createMeasure(goalId, measure)

                            // Move to the next day
                            currentDate = currentDate.plusDays(1)
                        }
                    } else {
                        // If no goal exists, create one first
                        val newGoal = GoalDto(
                            id = null,
                            patientId = selectedPatientId,
                            name = "Default Goal",
                            description = "Generated goal for measures",
                            frequency = "Daily",
                            duration = "1 month"
                        )

                        // Create the goal and then create measures
                        carePlanViewModel.createGoal(newGoal)

                        // Reload goals and measures after creating the goal
                        carePlanViewModel.loadCarePlansForPatient(selectedPatientId)
                    }

                    showDateRangeDialog = false
                }
            )
        } else {
            // If no patient is selected, just dismiss the dialog
            showDateRangeDialog = false
        }
    }

    // Add/Edit Goal Dialog
    if (carePlanState.showGoalDialog) {
        GoalDialog(
            goal = carePlanState.draftGoal!!,
            isEditing = carePlanState.isEditingGoal,
            onDismiss = { carePlanViewModel.closeGoalDialog() },
            onSave = { carePlanViewModel.saveGoal() },
            onUpdate = { updatedGoal: GoalDto ->
                carePlanViewModel.updateDraftGoal { updatedGoal }
            }
        )
    }

    // Add/Edit Measure Dialog
    if (carePlanState.showMeasureDialog) {
        MeasureDialog(
            measure = carePlanState.draftMeasure!!,
            isEditing = carePlanState.isEditingMeasure,
            onDismiss = { carePlanViewModel.closeMeasureDialog() },
            onSave = { carePlanViewModel.saveMeasure() },
            onUpdate = { updatedMeasure: MeasureDto ->
                carePlanViewModel.updateDraftMeasure { updatedMeasure }
            }
        )
    }

    // Goal Selection Dialog
    if (showGoalSelectionDialog) {
        Dialog(onDismissRequest = { showGoalSelectionDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Select Goal for Measure",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(carePlanState.goalsForPatient) { goal ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        goal.id?.let { goalId ->
                                            carePlanViewModel.openAddMeasureDialog(goalId)
                                            showGoalSelectionDialog = false
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text(
                                        text = goal.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = goal.description,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showGoalSelectionDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    // Error message snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    carePlanState.errorMessage?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            snackbarHostState.showSnackbar(
                message = errorMessage,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Long
            )
            carePlanViewModel.clearErrorMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun CarePlanListItem(
    carePlan: CarePlanDto,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = carePlan.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Start: ${carePlan.startDate.format(dateFormatter)} | End: ${carePlan.endDate.format(dateFormatter)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
    HorizontalDivider()
}

@Composable
fun GoalListItem(
    goal: GoalDto,
    measures: List<MeasureDto>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Frequency: ${goal.frequency} | Duration: ${goal.duration}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowBack else Icons.Default.ArrowForward,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            // Show measures if expanded
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Measures (${measures.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                measures.forEach { measure ->
                    MeasureListItem(measure = measure)
                }

                if (measures.isEmpty()) {
                    Text(
                        text = "No measures for this goal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
    HorizontalDivider()
}

@Composable
fun MeasureListItem(measure: MeasureDto) {
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (measure.isCompleted) Icons.Default.Done else Icons.Default.Close,
            contentDescription = if (measure.isCompleted) "Completed" else "Not Completed",
            tint = if (measure.isCompleted) Color.Green else Color.Red,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = measure.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Scheduled: ${measure.scheduledDateTime.format(dateTimeFormatter)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun CarePlanDetails(
    carePlan: CarePlanDto,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    selectedDate: LocalDate = LocalDate.now(),
    onEditGoalClick: (CarePlanGoalDto) -> Unit,
    onAddMeasureClick: (Long) -> Unit,
    onEditMeasureClick: (CarePlanMeasureDto, Long) -> Unit,
    onDeleteMeasureClick: (Long, Long) -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    // Filter measures for the selected date
    val measuresForSelectedDate = remember(carePlan.measures, selectedDate) {
        carePlan.measures.filter { measure ->
            try {
                // Get the date part of the LocalDateTime
                val measureDate = measure.scheduledDateTime.toLocalDate()
                measureDate == selectedDate
            } catch (e: Exception) {
                // If there's an error, include the measure anyway
                true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = carePlan.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Row {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Care Plan")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Care Plan")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Description: ${carePlan.description}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Period: ${carePlan.startDate.format(dateFormatter)} to ${carePlan.endDate.format(dateFormatter)}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Goal section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Goal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = { onEditGoalClick(carePlan.goal) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Goal")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Name: ${carePlan.goal.name}",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Description: ${carePlan.goal.description}",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Frequency: ${carePlan.goal.frequency}",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Duration: ${carePlan.goal.duration}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Measures section for selected date
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Measures for ${selectedDate.format(dateFormatter)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Add measure button
            Button(
                onClick = { onAddMeasureClick(carePlan.goal.id) },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Measure")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Measure")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (measuresForSelectedDate.isEmpty()) {
            Text(
                text = "No measures scheduled for this date",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            measuresForSelectedDate.forEach { measure ->
                MeasureItem(
                    measure = measure,
                    onEditClick = { onEditMeasureClick(measure, carePlan.goal.id) },
                    onDeleteClick = { onDeleteMeasureClick(carePlan.goal.id, measure.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MeasureItem(
    measure: CarePlanMeasureDto,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = measure.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Description: ${measure.description}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Scheduled: ${measure.scheduledDateTime.format(dateTimeFormatter)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row {
                Checkbox(
                    checked = measure.isCompleted,
                    onCheckedChange = null, // In a real implementation, this would update the measure
                    enabled = false
                )

                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Measure")
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Measure")
                }
            }
        }
    }
}

@Composable
fun CarePlanDialog(
    carePlan: CarePlanDto,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onUpdate: (CarePlanDto) -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }

    var title by remember { mutableStateOf(carePlan.title) }
    var description by remember { mutableStateOf(carePlan.description) }
    var startDateString by remember { mutableStateOf(carePlan.startDate.format(dateFormatter)) }
    var endDateString by remember { mutableStateOf(carePlan.endDate.format(dateFormatter)) }
    var goalName by remember { mutableStateOf(carePlan.goal.name) }
    var goalDescription by remember { mutableStateOf(carePlan.goal.description) }
    var goalFrequency by remember { mutableStateOf(carePlan.goal.frequency) }
    var goalDuration by remember { mutableStateOf(carePlan.goal.duration) }

    // Measure fields
    var measureName by remember { mutableStateOf("") }
    var measureDescription by remember { mutableStateOf("") }
    var measureScheduledDateTime by remember { mutableStateOf(LocalDateTime.now().format(dateTimeFormatter)) }

    var showError by remember { mutableStateOf(false) }
    var measureError by remember { mutableStateOf(false) }

    // Track measures to be added
    val measures = remember { mutableStateListOf<CarePlanMeasureDto>() }

    // Initialize with existing measures if editing
    LaunchedEffect(carePlan) {
        measures.clear()
        measures.addAll(carePlan.measures)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isEditing) "Edit Care Plan" else "Add Care Plan",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { 
                        title = it
                        showError = false
                        onUpdate(carePlan.copy(title = it))
                    },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && title.isBlank()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { 
                        description = it
                        showError = false
                        onUpdate(carePlan.copy(description = it))
                    },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError && description.isBlank()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = startDateString,
                    onValueChange = { 
                        startDateString = it
                        showError = false
                        try {
                            val parsedDate = LocalDate.parse(it, dateFormatter)
                            onUpdate(carePlan.copy(startDate = parsedDate))
                        } catch (e: Exception) {
                            // Invalid date format, don't update the model
                        }
                    },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && startDateString.isBlank()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = endDateString,
                    onValueChange = { 
                        endDateString = it
                        showError = false
                        try {
                            val parsedDate = LocalDate.parse(it, dateFormatter)
                            onUpdate(carePlan.copy(endDate = parsedDate))
                        } catch (e: Exception) {
                            // Invalid date format, don't update the model
                        }
                    },
                    label = { Text("End Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && endDateString.isBlank()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Goal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = goalName,
                    onValueChange = { 
                        goalName = it
                        showError = false
                        onUpdate(carePlan.copy(
                            goal = carePlan.goal.copy(name = it)
                        ))
                    },
                    label = { Text("Goal Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && goalName.isBlank()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = goalDescription,
                    onValueChange = { 
                        goalDescription = it
                        showError = false
                        onUpdate(carePlan.copy(
                            goal = carePlan.goal.copy(description = it)
                        ))
                    },
                    label = { Text("Goal Description") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError && goalDescription.isBlank()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = goalFrequency,
                    onValueChange = { 
                        goalFrequency = it
                        showError = false
                        onUpdate(carePlan.copy(
                            goal = carePlan.goal.copy(frequency = it)
                        ))
                    },
                    label = { Text("Goal Frequency") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && goalFrequency.isBlank()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = goalDuration,
                    onValueChange = { 
                        goalDuration = it
                        showError = false
                        onUpdate(carePlan.copy(
                            goal = carePlan.goal.copy(duration = it)
                        ))
                    },
                    label = { Text("Goal Duration") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && goalDuration.isBlank()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Measures section
                Text(
                    text = "Measures",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Display existing measures
                if (measures.isNotEmpty()) {
                    Text(
                        text = "Current Measures:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    measures.forEachIndexed { index, measure ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = measure.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = measure.description,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Scheduled: ${measure.scheduledDateTime.format(dateTimeFormatter)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(
                                onClick = {
                                    measures.removeAt(index)
                                    onUpdate(carePlan.copy(measures = measures.toList()))
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Measure")
                            }
                        }
                        if (index < measures.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Add new measure
                Text(
                    text = "Add New Measure:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = measureName,
                    onValueChange = { 
                        measureName = it
                        measureError = false
                    },
                    label = { Text("Measure Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = measureError && measureName.isBlank()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = measureDescription,
                    onValueChange = { 
                        measureDescription = it
                        measureError = false
                    },
                    label = { Text("Measure Description") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = measureError && measureDescription.isBlank()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = measureScheduledDateTime,
                    onValueChange = { 
                        measureScheduledDateTime = it
                        measureError = false
                    },
                    label = { Text("Scheduled Date/Time (YYYY-MM-DD HH:MM)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = measureError && measureScheduledDateTime.isBlank()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (measureName.isBlank() || measureDescription.isBlank() || measureScheduledDateTime.isBlank()) {
                            measureError = true
                        } else {
                            try {
                                val scheduledDateTime = LocalDateTime.parse(measureScheduledDateTime, dateTimeFormatter)
                                val newMeasure = CarePlanMeasureDto(
                                    id = 0, // New measure
                                    name = measureName,
                                    description = measureDescription,
                                    scheduledDateTime = scheduledDateTime,
                                    isCompleted = false
                                )
                                measures.add(newMeasure)
                                onUpdate(carePlan.copy(measures = measures.toList()))

                                // Clear fields for next measure
                                measureName = ""
                                measureDescription = ""
                                measureScheduledDateTime = LocalDateTime.now().format(dateTimeFormatter)
                                measureError = false
                            } catch (e: Exception) {
                                measureError = true
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Add Measure")
                }

                if (measureError) {
                    Text(
                        text = "Please fill all measure fields with valid values",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (title.isBlank() || description.isBlank() || 
                                startDateString.isBlank() || endDateString.isBlank() ||
                                goalName.isBlank() || goalDescription.isBlank() ||
                                goalFrequency.isBlank() || goalDuration.isBlank()) {
                                showError = true
                            } else if (measures.isEmpty()) {
                                // Ensure at least one measure is added
                                measureError = true
                            } else {
                                // Validate date formats
                                try {
                                    LocalDate.parse(startDateString, dateFormatter)
                                    LocalDate.parse(endDateString, dateFormatter)
                                    onSave()
                                } catch (e: Exception) {
                                    showError = true
                                }
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Dialog for selecting a date range to add measures for an entire period.
 */
@Composable
fun DateRangeDialog(
    startDate: LocalDate,
    endDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    var newStartDate by remember { mutableStateOf(startDate) }
    var newEndDate by remember { mutableStateOf(endDate) }
    var showError by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Add Measures for Period",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Start Date",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { newStartDate = newStartDate.minusDays(1) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Previous Day")
                    }

                    Text(
                        text = newStartDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center
                    )

                    IconButton(onClick = { newStartDate = newStartDate.plusDays(1) }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next Day")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "End Date",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { newEndDate = newEndDate.minusDays(1) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Previous Day")
                    }

                    Text(
                        text = newEndDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center
                    )

                    IconButton(onClick = { newEndDate = newEndDate.plusDays(1) }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next Day")
                    }
                }

                if (showError) {
                    Text(
                        text = "End date must be after start date",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (newEndDate.isBefore(newStartDate)) {
                                showError = true
                            } else {
                                onConfirm(newStartDate, newEndDate)
                            }
                        }
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}


/**
 * Dialog for adding or editing a measure.
 */
@Composable
fun MeasureDialog(
    measure: MeasureDto,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onUpdate: (MeasureDto) -> Unit
) {
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }

    var name by remember { mutableStateOf(measure.name) }
    var description by remember { mutableStateOf(measure.description) }
    var scheduledDateTimeString by remember { mutableStateOf(measure.scheduledDateTime.format(dateTimeFormatter)) }
    var isCompleted by remember { mutableStateOf(measure.isCompleted) }

    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isEditing) "Edit Measure" else "Add Measure",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        showError = false
                        onUpdate(measure.copy(name = it))
                    },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && name.isBlank()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { 
                        description = it
                        showError = false
                        onUpdate(measure.copy(description = it))
                    },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError && description.isBlank()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = scheduledDateTimeString,
                    onValueChange = { 
                        scheduledDateTimeString = it
                        showError = false
                        try {
                            val parsedDateTime = LocalDateTime.parse(it, dateTimeFormatter)
                            onUpdate(measure.copy(scheduledDateTime = parsedDateTime))
                        } catch (e: Exception) {
                            // Invalid date format, don't update the model
                        }
                    },
                    label = { Text("Scheduled Date/Time (YYYY-MM-DD HH:MM)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && scheduledDateTimeString.isBlank()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isCompleted,
                        onCheckedChange = { 
                            isCompleted = it
                            onUpdate(measure.copy(isCompleted = it))
                        }
                    )
                    Text("Completed")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (name.isBlank() || description.isBlank() || scheduledDateTimeString.isBlank()) {
                                showError = true
                            } else {
                                try {
                                    LocalDateTime.parse(scheduledDateTimeString, dateTimeFormatter)
                                    onSave()
                                } catch (e: Exception) {
                                    showError = true
                                }
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Dialog for adding or editing a goal.
 */
@Composable
fun GoalDialog(
    goal: GoalDto,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onUpdate: (GoalDto) -> Unit
) {
    var name by remember { mutableStateOf(goal.name) }
    var description by remember { mutableStateOf(goal.description) }
    var frequency by remember { mutableStateOf(goal.frequency) }
    var duration by remember { mutableStateOf(goal.duration) }

    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isEditing) "Edit Goal" else "Add Goal",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        showError = false
                        onUpdate(goal.copy(name = it))
                    },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && name.isBlank()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { 
                        description = it
                        showError = false
                        onUpdate(goal.copy(description = it))
                    },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError && description.isBlank()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = frequency,
                    onValueChange = { 
                        frequency = it
                        showError = false
                        onUpdate(goal.copy(frequency = it))
                    },
                    label = { Text("Frequency") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && frequency.isBlank()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = duration,
                    onValueChange = { 
                        duration = it
                        showError = false
                        onUpdate(goal.copy(duration = it))
                    },
                    label = { Text("Duration") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && duration.isBlank()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (name.isBlank() || description.isBlank() || 
                                frequency.isBlank() || duration.isBlank()) {
                                showError = true
                            } else {
                                onSave()
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
