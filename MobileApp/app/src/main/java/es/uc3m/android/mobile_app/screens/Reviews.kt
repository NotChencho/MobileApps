package es.uc3m.android.mobile_app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import es.uc3m.android.mobile_app.NavGraph
import es.uc3m.android.mobile_app.viewmodel.MyViewModel


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

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Recent Reviews",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn {
            items(reviews) { review ->
                ReviewCard(review.user, review.restaurant, review.comment, navController)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ReviewCard(user: String, restaurant: String, comment: String, navController: NavHostController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "User Icon",
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 8.dp),
                tint = Color.Gray
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = user, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
                Text(text = "Visited: $restaurant", fontSize = 14.sp)
                Text(text = "Comment: $comment", fontSize = 12.sp)
            }

            Button(onClick = {
                navController.navigate(NavGraph.ReviewDetails.createRoute(user, restaurant))
            }) {
                Text(text = "View")
            }
        }
    }
}
