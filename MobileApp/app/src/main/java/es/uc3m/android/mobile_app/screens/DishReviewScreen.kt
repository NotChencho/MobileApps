package es.uc3m.android.mobile_app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import es.uc3m.android.mobile_app.viewmodel.MyViewModel
import es.uc3m.android.mobile_app.viewmodel.Review

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishReviewScreen(
    navController: NavHostController,
    viewModel: MyViewModel = viewModel(),
    restaurantId: String = "", // Make it optional with default empty string
    restaurantName: String,
    dishName: String
) {
    val user by viewModel.user.collectAsState()
    val userEmail = user?.email ?: "Unknown"

    var title by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var submitSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Review for $dishName",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = "at $restaurantName",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Title input
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Review Title") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        // Rating selector
        Text(
            text = "Rating",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            repeat(5) { index ->
                IconButton(onClick = { rating = index + 1 }) {
                    Icon(
                        imageVector = if (index < rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Star ${index + 1}",
                        tint = if (index < rating) Color(0xFFFFC107) else Color.Gray
                    )
                }
            }
        }

        // Comment input
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Your Review") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(bottom = 24.dp),
            maxLines = 5
        )

        // Submit button
        Button(
            onClick = {
                if (rating == 0) {
                    submitError = "Please select a rating"
                    return@Button
                }

                if (title.isBlank()) {
                    submitError = "Please enter a title for your review"
                    return@Button
                }

                isSubmitting = true
                submitError = null

                val review = Review(
                    user = userEmail,
                    restaurant = restaurantName,
                    dish = dishName,
                    rating = rating,
                    comment = comment,
                    title = title
                )

                viewModel.addDishReview(review, onSuccess = {
                    isSubmitting = false
                    submitSuccess = true
                    // Navigate back after short delay
                    navController.popBackStack()
                }, onError = { error ->
                    isSubmitting = false
                    submitError = error
                })
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            enabled = !isSubmitting
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Text("Submit Review")
            }
        }

        // Error message
        submitError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Cancel button
        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Cancel")
        }
    }
}