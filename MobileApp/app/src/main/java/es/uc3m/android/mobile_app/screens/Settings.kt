package es.uc3m.android.mobile_app.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.mobile_app.viewmodel.MyViewModel
import es.uc3m.android.mobile_app.viewmodel.UserPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.snapshots.SnapshotStateList

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    source: String? = null,
    viewModel: MyViewModel = viewModel()
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val scrollState = rememberScrollState()

    var isSaving by remember { mutableStateOf(false) }
    val currentPreferences by viewModel.userPreferences.collectAsState()
    val saveStatus by viewModel.savePreferencesStatus.collectAsState()

    source?.let {
        Toast.makeText(context, "Opened from: $it", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(saveStatus) {
        when (saveStatus) {
            is es.uc3m.android.mobile_app.viewmodel.SaveStatus.Success -> {
                Toast.makeText(context, "Preferences saved successfully", Toast.LENGTH_SHORT).show()
                isSaving = false
            }
            is es.uc3m.android.mobile_app.viewmodel.SaveStatus.Error -> {
                val errorMsg = (saveStatus as es.uc3m.android.mobile_app.viewmodel.SaveStatus.Error).message
                Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                isSaving = false
            }
            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadUserPreferences()
    }

    val selectedDate = datePickerState.selectedDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    } ?: LocalDate.now()

    var selectedPrice by remember(currentPreferences) {
        mutableStateOf(currentPreferences?.priceRange ?: "$")
    }
    var selectedFood by remember(currentPreferences) {
        mutableStateOf(currentPreferences?.foodType ?: "Italian")
    }

    val allergyOptions = listOf("Peanut Allergy", "Lactose Intolerant", "Gluten-Free", "None")
    val otherOptions = listOf("Takeout", "Reservations", "Outdoor Seating", "Delivery")
    val selectedAllergies = remember { mutableStateListOf<String>() }
    val selectedOthers = remember { mutableStateListOf<String>() }

    LaunchedEffect(currentPreferences) {
        selectedAllergies.clear()
        selectedAllergies.addAll(currentPreferences?.allergyPreferences ?: emptyList())
        selectedOthers.clear()
        selectedOthers.addAll(currentPreferences?.otherPreferences ?: emptyList())
    }

    val priceOptions = listOf("$", "$$", "$$$")
    val foodOptions = listOf("Italian", "Mexican", "Japanese")

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Select The Dates", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(onClick = { showDatePicker = true }) {
            Icon(Icons.Default.CalendarToday, contentDescription = "Calendar")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Preferences", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        DropdownMenuComponent("Price", priceOptions, selectedPrice) { selectedPrice = it }
        DropdownMenuComponent("Type of food", foodOptions, selectedFood) { selectedFood = it }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Allergy Preferences", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        FlowCheckboxGrid(options = allergyOptions, selectedItems = selectedAllergies)

        Spacer(modifier = Modifier.height(16.dp))
        Text("Other Preferences", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        FlowCheckboxGrid(options = otherOptions, selectedItems = selectedOthers)

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                isSaving = true
                val userPreferences = UserPreferences(
                    foodType = selectedFood,
                    priceRange = selectedPrice,
                    allergyPreferences = selectedAllergies.toList(),
                    otherPreferences = selectedOthers.toList(),
                    date = selectedDate.format(DateTimeFormatter.ISO_DATE)
                )
                viewModel.saveUserPreferences(userPreferences)
                Toast.makeText(context, "Saving preferences...", Toast.LENGTH_SHORT).show()
            },
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Save Preferences")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowCheckboxGrid(options: List<String>, selectedItems: SnapshotStateList<String>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = option in selectedItems,
                    onCheckedChange = { checked ->
                        if (checked) selectedItems.add(option)
                        else selectedItems.remove(option)
                    }
                )
                Text(option)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuComponent(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().align(Alignment.CenterHorizontally)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        onOptionSelected(option)
                        expanded = false
                    })
                }
            }
        }
    }
}