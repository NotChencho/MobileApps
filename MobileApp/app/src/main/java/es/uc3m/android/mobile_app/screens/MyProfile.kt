package es.uc3m.android.mobile_app.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import es.uc3m.android.mobile_app.viewmodel.MyViewModel
import es.uc3m.android.mobile_app.viewmodel.Review

@Composable
fun MyProfileScreen(
    navController: NavHostController,
    viewModel: MyViewModel = viewModel()
) {
    val user by viewModel.user.collectAsState()
    val allReviews by viewModel.reviews.collectAsState()

    // Load reviews if not loaded yet
    LaunchedEffect(Unit) {
        viewModel.loadReviews()
    }

    // Filter reviews only for the current user
    val userEmail = user?.email ?: "Unknown"
    val userReviews = allReviews.filter { it.user == userEmail }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "User Icon",
            tint = Color.Gray,
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 16.dp)
        )

        // Email
        Text(
            text = userEmail,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Section Title
        Text(
            text = "My Reviews",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Logout
        Button(
            onClick = {
                viewModel.logout()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(0.6f)
        ) {
            Text("Logout", color = Color.White)
        }

        // Reviews
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(userReviews) { review ->
                ReviewItem(review)
                Divider()
            }
        }
    }
}

@Composable
fun ReviewItem(review: Review) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = review.restaurant,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = "Rating: ${review.rating} ⭐",
            fontSize = 14.sp
        )
        Text(
            text = review.comment,
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}
