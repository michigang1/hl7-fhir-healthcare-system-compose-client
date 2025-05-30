package presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import data.model.PatientDto


typealias OnDialogDismissRequest = () -> Unit
typealias OnPatientAddRequest = (patient: PatientDto) -> Unit

@Composable
fun PatientsScreen(viewModel: PatientViewModel) {
    val uiState = viewModel.uiState // Введение переменной для uiState

    Row(Modifier.fillMaxSize()) {
        PatientListPane(
            modifier = Modifier
                .weight(0.33f)
                .fillMaxHeight()
                // Убрано боковое правое padding, так как VerticalDivider теперь его заменяет
                .padding(top = 8.dp, start = 8.dp, bottom = 8.dp),
            patients = uiState.patientsList,
            selectedPatient = uiState.selectedPatient,
            isDetailPaneEditing = uiState.isEditing,
            onAddPatient = { viewModel.openAddPatientDialog() },
            onPatientSelect = { patient -> viewModel.selectPatient(patient.id) },
            onPatientDelete = { patient -> viewModel.deletePatient(patient.id) }
        )

        VerticalDivider(
            modifier = Modifier.fillMaxHeight().width(1.dp),
            thickness = 1.dp, // Можно сделать еще тоньше, например 0.5.dp
            color = MaterialTheme.colorScheme.outlineVariant // Используем нейтральный цвет
        )

        PatientDetailPane(
            modifier = Modifier
                .weight(0.67f)
                .fillMaxHeight()
                // Убрано боковое левое padding
                .padding(top = 8.dp, end = 8.dp, bottom = 8.dp),
            patientToDisplay = if (uiState.isEditing) uiState.draftPatient else uiState.selectedPatient,
            isEditing = uiState.isEditing,
            onEditToggle = { viewModel.startEditing() },
            onSave = { viewModel.saveChanges() },
            onCancel = { viewModel.cancelEditing() },
            onFieldChange = { update -> viewModel.updateDraftPatient(update) }
        )
    }

    if (uiState.showAddPatientDialog) {
        AddPatientDialog(
            onDismiss = { viewModel.closeAddPatientDialog() },
            onAddPatient = { patient -> viewModel.addPatient(patient) }
        )
    }
}


@Composable
private fun PatientListPane(
    modifier: Modifier = Modifier,
    patients: List<PatientDto>,
    selectedPatient: PatientDto?,
    isDetailPaneEditing: Boolean,
    onAddPatient: () -> Unit,
    onPatientSelect: (PatientDto) -> Unit,
    onPatientDelete: (PatientDto) -> Unit
) {
    Column(modifier = modifier.padding(horizontal = 8.dp)) { // Добавлен общий горизонтальный padding для содержимого панели
        Button(
            onClick = onAddPatient,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp) // Обновлен padding кнопки
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Add Patient",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Add Patient")
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(patients, key = { it.id }) { patient ->
                PatientListItem(
                    patient = patient,
                    isSelected = selectedPatient?.id == patient.id && !isDetailPaneEditing,
                    onPatientClick = { onPatientSelect(patient) },
                    onDeleteClick = { onPatientDelete(patient) },
                    isDetailPaneEditing = isDetailPaneEditing
                )
                // Минималистичный горизонтальный разделитель
                Divider(
                    thickness = 0.5.dp, // Тонкий разделитель
                    color = MaterialTheme.colorScheme.outlineVariant, // Нейтральный цвет
                    modifier = Modifier.padding(horizontal = 4.dp) // Небольшой отступ для разделителя
                )
            }
        }
    }
}


@Composable
private fun PatientDetailPane(
    modifier: Modifier = Modifier,
    patientToDisplay: PatientDto?,
    isEditing: Boolean,
    onEditToggle: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onFieldChange: ((PatientDto) -> PatientDto) -> Unit
) {
    Box(
        modifier = modifier.padding(horizontal = 8.dp), // Добавлен общий горизонтальный padding
        contentAlignment = Alignment.TopStart
    ) {
        if (patientToDisplay != null) {
            PatientDetailCard( // PatientDetailCard будет использовать свой внутренний padding
                patient = patientToDisplay,
                isEditing = isEditing,
                onEditToggle = onEditToggle,
                onSave = onSave,
                onCancel = onCancel,
                onFieldChange = onFieldChange
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Select a patient from the list to view details.",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun PatientListItem(
    patient: PatientDto,
    isSelected: Boolean,
    onPatientClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isDetailPaneEditing: Boolean // Получаем состояние редактирования панели деталей
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isDetailPaneEditing) { onPatientClick() } // Блокируем клик, если идет редактирование
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${patient.name} ${patient.surname}",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Room ${patient.roomNo}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(
            onClick = onDeleteClick,
            enabled = !isDetailPaneEditing
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete Patient",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun EditModeHeaderActions(onSave: () -> Unit, onCancel: () -> Unit) {
    Row {
        IconButton(onClick = onSave) {
            Icon(
                Icons.Filled.Done, // Используйте более подходящую иконку, например, Icons.Filled.Done
                contentDescription = "Save Changes",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onCancel) {
            Icon(
                Icons.Filled.Close, // Рассмотрите Icons.Filled.Close или Icons.Filled.Cancel
                contentDescription = "Cancel Edit",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ViewModeHeaderActions(onEditToggle: () -> Unit) {
    Button(onClick = onEditToggle) {
        Icon(
            Icons.Filled.Edit,
            contentDescription = "Edit Patient",
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text("Edit")
    }
}

@Composable
private fun PatientDetailCardHeader(
    isEditing: Boolean,
    onEditToggle: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isEditing) "EDIT PATIENT PROFILE" else "PATIENT PROFILE",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        if (isEditing) {
            EditModeHeaderActions(onSave = onSave, onCancel = onCancel)
        } else {
            ViewModeHeaderActions(onEditToggle = onEditToggle)
        }
    }
}

@Composable
private fun PatientDetailCard(
    patient: PatientDto,
    isEditing: Boolean,
    onEditToggle: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onFieldChange: ((PatientDto) -> PatientDto) -> Unit
) {
    Card(modifier = Modifier.fillMaxSize().padding(top = 8.dp /* Если нужен отступ сверху внутри панели */)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PatientDetailCardHeader(
                isEditing = isEditing,
                onEditToggle = onEditToggle,
                onSave = onSave,
                onCancel = onCancel
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            EditableInfoRow("Name", patient.name, isEditing) { updatedValue ->
                onFieldChange { it.copy(name = updatedValue) }
            }
            EditableInfoRow("Surname", patient.surname, isEditing) { updatedValue ->
                onFieldChange { it.copy(surname = updatedValue) }
            }
            EditableInfoRow("Room No", patient.roomNo, isEditing) { updatedValue ->
                onFieldChange { it.copy(roomNo = updatedValue) }
            }
            EditableInfoRow("Date of Birth", patient.dateOfBirth, isEditing) { updatedValue ->
                onFieldChange { it.copy(dateOfBirth = updatedValue) }
            }
            EditableInfoRow("Gender", patient.gender, isEditing) { updatedValue ->
                onFieldChange { it.copy(gender = updatedValue) }
            }
            EditableInfoRow("Address", patient.address, isEditing) { updatedValue ->
                onFieldChange { it.copy(address = updatedValue) }
            }
            EditableInfoRow("Email", patient.email, isEditing) { updatedValue ->
                onFieldChange { it.copy(email = updatedValue) }
            }
            EditableInfoRow("Phone", patient.phone, isEditing) { updatedValue ->
                onFieldChange { it.copy(phone = updatedValue) }
            }
            EditableInfoRow("Identifier", patient.identifier.toString(), isEditing) { updatedValue ->
                onFieldChange { it.copy(identifier = updatedValue.toLongOrNull() ?: it.identifier) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientDialog(
    onDismiss: OnDialogDismissRequest,
    onAddPatient: OnPatientAddRequest
) {
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var roomNo by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var identifierText by remember { mutableStateOf("") }
    var organizationIdText by remember { mutableStateOf("") }

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("Please fill all required fields.") }


    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Add New Patient", style = MaterialTheme.typography.headlineSmall)

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
                    label = { Text("Name*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && name.isBlank()
                )
                OutlinedTextField(
                    value = surname,
                    onValueChange = { surname = it; showError = false },
                    label = { Text("Surname*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && surname.isBlank()
                )
                OutlinedTextField(
                    value = roomNo,
                    onValueChange = { roomNo = it; showError = false },
                    label = { Text("Room No.") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && roomNo.isBlank()
                )
                OutlinedTextField(
                    value = dateOfBirth,
                    onValueChange = { dateOfBirth = it; showError = false },
                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && dateOfBirth.isBlank()
                )
                OutlinedTextField(
                    value = gender,
                    onValueChange = { gender = it; showError = false },
                    label = { Text("Gender") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && gender.isBlank()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it; showError = false },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && address.isBlank()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; showError = false },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && email.isBlank()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; showError = false },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && phone.isBlank()
                )
                OutlinedTextField(
                    value = identifierText,
                    onValueChange = { identifierText = it; showError = false },
                    label = { Text("Identifier*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && identifierText.isBlank()
                )
                OutlinedTextField(
                    value = organizationIdText,
                    onValueChange = { organizationIdText = it; showError = false },
                    label = { Text("Organization ID*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && organizationIdText.isBlank()
                )


                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (name.isNotBlank() && surname.isNotBlank() &&
                            roomNo.isNotBlank() && dateOfBirth.isNotBlank() &&
                            gender.isNotBlank() && address.isNotBlank() &&
                            email.isNotBlank() && phone.isNotBlank() && // Добавил phone в проверку обязательных полей
                            identifierText.isNotBlank() && organizationIdText.isNotBlank()
                        ) {
                            val identifier = identifierText.toLongOrNull()
                            val organizationId = organizationIdText.toLongOrNull()

                            if (identifier == null || organizationId == null) {
                                errorMessage = "Identifier and Organization ID must be valid numbers."
                                showError = true
                            } else {
                                val newPatient = PatientDto(
                                    id = 0L,
                                    name = name.trim(),
                                    surname = surname.trim(),
                                    roomNo = roomNo.trim(),
                                    dateOfBirth = dateOfBirth.trim(),
                                    gender = gender.trim(),
                                    address = address.trim(),
                                    email = email.trim(),
                                    phone = phone.trim(),
                                    identifier = identifier,
                                    organizationId = organizationId
                                )
                                onAddPatient(newPatient)
                                // onDismiss() // Диалог закроется автоматически при успешном saveChanges из ViewModel
                            }
                        } else {
                            errorMessage = "Please fill all fields marked with *."
                            showError = true
                        }
                    }) {
                        Text("Add Patient")
                    }
                }
            }
        }
    }
}

@Composable
fun EditableInfoRow(
    label: String,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.4f)
        )
        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(0.6f),
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = true
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(0.6f)
            )
        }
    }
}