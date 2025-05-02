package es.uc3m.android.mobile_app.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, source: String? = null) {  // ✅ Fixed function signature
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // Display source if provided
    source?.let {
        Toast.makeText(context, "Opened from: $it", Toast.LENGTH_SHORT).show()
    }

    // Get the selected date from the state
    val selectedDate = datePickerState.selectedDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    } ?: LocalDate.now()

    // Dropdown Options
    val priceOptions = listOf("$", "$$", "$$$", "$$$$")
    val allergyOptions = listOf("None", "Gluten-Free", "Peanut Allergy", "Lactose Intolerant")
    val foodOptions = listOf("Italian", "Mexican", "Japanese", "Vegan", "Fast Food")
    val otherOptions = listOf("Outdoor Seating", "Delivery", "Takeout", "Reservations")

    var selectedPrice by remember { mutableStateOf(priceOptions[0]) }
    var selectedAllergy by remember { mutableStateOf(allergyOptions[0]) }
    var selectedFood by remember { mutableStateOf(foodOptions[0]) }
    var selectedOther by remember { mutableStateOf(otherOptions[0]) }

    // Date Picker Dialog
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
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Select The Dates", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Date Picker Button
        OutlinedButton(onClick = { showDatePicker = true }) {
            Icon(Icons.Default.CalendarToday, contentDescription = "Calendar")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Preferences", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        DropdownMenuComponent("Price", priceOptions, selectedPrice) { selectedPrice = it }
        DropdownMenuComponent("Allergies", allergyOptions, selectedAllergy) { selectedAllergy = it }
        DropdownMenuComponent("Type of food", foodOptions, selectedFood) { selectedFood = it }
        DropdownMenuComponent("Others", otherOptions, selectedOther) { selectedOther = it }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            Toast.makeText(
                context, "Filters:\n" +
                        "Date: ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}\n" +
                        "Price: $selectedPrice\n" +
                        "Allergy: $selectedAllergy\n" +
                        "Food: $selectedFood\n" +
                        "Other: $selectedOther",
                Toast.LENGTH_LONG
            ).show()
        }) {
            Text("Confirm Selection")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuComponent(label: String, options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
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