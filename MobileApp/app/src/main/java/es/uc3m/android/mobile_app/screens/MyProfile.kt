package es.uc3m.android.mobile_app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
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
import es.uc3m.android.mobile_app.viewmodel.MyViewModel
import es.uc3m.android.mobile_app.viewmodel.Review
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import es.uc3m.android.mobile_app.NavGraph
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage


@Composable
fun MyProfileScreen(
    navController: NavHostController,
    viewModel: MyViewModel = viewModel()
) {
    val user by viewModel.user.collectAsState()
    val allReviews by viewModel.reviews.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf<String?>(null) }

    // Load reviews if not loaded yet
    LaunchedEffect(Unit) {
        viewModel.loadReviews()
    }

    // Filter reviews only for the current user
    val userEmail = user?.email ?: "Unknown"

    val followersCount = remember { mutableStateOf(0) }
    val followingCount = remember { mutableStateOf(0) }

    LaunchedEffect(userEmail) {
        viewModel.getFollowers(userEmail) { list ->
            followersCount.value = list.size
        }
        viewModel.getFollowing(userEmail) { list ->
            followingCount.value = list.size
        }
    }

    val userReviews = allReviews.filter { it.user == userEmail }
        .sortedByDescending { it.timestamp }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = "Followers: ${followersCount.value}",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    viewModel.getFollowers(userEmail) { list: List<String> ->
                        val route = NavGraph.UserList.createRoute("Followers", list)
                        navController.navigate(route)
                    }
                }
            )

            Text(
                text = "Following: ${followingCount.value}",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    viewModel.getFollowing(userEmail) { list: List<String> ->
                        val route = NavGraph.UserList.createRoute("Following", list)
                        navController.navigate(route)
                    }
                }
            )
        }

        // Logout Button
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

        // Section Title
        Text(
            text = "My Reviews (${userReviews.size})",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 24.dp, bottom = 12.dp)
        )

        // Reviews
        if (userReviews.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "You haven't written any reviews yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(userReviews) { review ->
                    ReviewItemCard(
                        review = review,
                        onClickReview = {
                            navController.navigate("dish_reviews_list/${review.restaurant}/${review.dish}")
                        },
                        onDeleteReview = {
                            showDeleteConfirmation = review.id
                        }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteConfirmation?.let { reviewId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Delete Review") },
            text = { Text("Are you sure you want to delete this review? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReview(
                            reviewId = reviewId,
                            onSuccess = {
                                showDeleteConfirmation = null
                            },
                            onError = { /* Handle error */ }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewItemCard(
    review: Review,
    onClickReview: () -> Unit,
    onDeleteReview: () -> Unit
) {
    Card(
        onClick = onClickReview,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Display photo if available
            if (!review.photoUrl.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                ) {
                    AsyncImage(
                        model = review.photoUrl,
                        contentDescription = "Review photo of ${review.dish}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Restaurant and dish info
            Text(
                text = "${review.dish} at ${review.restaurant}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rating and title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(review.rating) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = review.title,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Review comment
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Format timestamp
                val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                val dateString = formatter.format(Date(review.timestamp))

                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                // Delete button
                IconButton(onClick = onDeleteReview) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Review",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}