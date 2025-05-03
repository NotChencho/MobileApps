package es.uc3m.android.mobile_app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import es.uc3m.android.mobile_app.NavGraph
import es.uc3m.android.mobile_app.viewmodel.MyViewModel
import es.uc3m.android.mobile_app.viewmodel.Review
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReviewsScreen(
    navController: NavHostController,
    viewModel: MyViewModel = viewModel()
) {
    val reviews by viewModel.reviews.collectAsState()

    // Load reviews from Firestore when the screen is composed
    LaunchedEffect(Unit) {
        viewModel.loadReviews()
    }

    // Sort reviews by timestamp (most recent first)
    val sortedReviews = reviews.sortedByDescending { it.timestamp }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Recent Reviews",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn {
            items(sortedReviews) { review ->
                ReviewCard(review, navController)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ReviewCard(review: Review, navController: NavHostController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // User info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "User Icon",
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 8.dp),
                    tint = Color.Gray
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.user,
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Format timestamp
                    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    val dateString = formatter.format(Date(review.timestamp))

                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Restaurant and dish info
            Text(
                text = "${review.restaurant} • ${review.dish}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            // Rating row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                repeat(review.rating) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Star",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = review.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Review comment
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // View details button
            Button(
                onClick = {
                    navController.navigate("dish_reviews_list/${review.restaurant}/${review.dish}")
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
            ) {
                Text(text = "View All Reviews")
            }
        }
    }
}