package es.uc3m.android.mobile_app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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
import es.uc3m.android.mobile_app.NavGraph
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PublicProfileScreen(
    navController: NavHostController,
    email: String,
    viewModel: MyViewModel = viewModel()
) {
    val allReviews by viewModel.reviews.collectAsState()
    val currentUser by viewModel.user.collectAsState()

    val followersCount = remember { mutableStateOf(0) }
    val followingCount = remember { mutableStateOf(0) }
    val isFollowing = remember { mutableStateOf(false) }

    val showFollowButton = currentUser?.email != null && currentUser?.email != email

    LaunchedEffect(email) {
        viewModel.getFollowers(email) { followers ->
            followersCount.value = followers.size
        }
        viewModel.getFollowing(email) { following ->
            followingCount.value = following.size
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadReviews()
    }

    LaunchedEffect(currentUser?.email, email) {
        if (currentUser?.email != null && currentUser?.email != email) {
            viewModel.isFollowing(currentUser!!.email!!, email) {
                isFollowing.value = it
            }
        }
    }

    val userReviews = allReviews.filter { it.user == email }
        .sortedByDescending { it.timestamp }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "User Icon",
            tint = Color.Gray,
            modifier = Modifier
                .size(100.dp)
                .padding(bottom = 8.dp)
        )

        Text(text = email, style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Followers: ${followersCount.value}",
                modifier = Modifier.clickable {
                    viewModel.getFollowers(email) { list: List<String> ->
                        val route = NavGraph.UserList.createRoute("Followers", list)
                        navController.navigate(route)
                    }
                },
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Following: ${followingCount.value}",
                modifier = Modifier.clickable {
                    viewModel.getFollowing(email) { list: List<String> ->
                        val route = NavGraph.UserList.createRoute("Following", list)
                        navController.navigate(route)
                    }
                },
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (showFollowButton) {
            Button(
                onClick = {
                    val myEmail = currentUser?.email ?: return@Button
                    if (isFollowing.value) {
                        viewModel.unfollowUser(myEmail, email)
                        isFollowing.value = false
                        followersCount.value -= 1
                    } else {
                        viewModel.followUser(myEmail, email)
                        isFollowing.value = true
                        followersCount.value += 1
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing.value) Color.Gray else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Text(
                    text = if (isFollowing.value) "Unfollow" else "Follow",
                    color = Color.White
                )
            }
        }

        Text(
            text = "Reviews (${userReviews.size})",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 24.dp, bottom = 12.dp)
        )

        if (userReviews.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("This user hasn't written any reviews yet", color = Color.Gray)
            }
        } else {
            LazyColumn {
                items(userReviews) { review ->
                    ReviewSummaryCard(review)
                }
            }
        }
    }
}

@Composable
fun ReviewSummaryCard(review: Review) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${review.dish} at ${review.restaurant}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(review.rating) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = review.title, style = MaterialTheme.typography.titleSmall)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(text = review.comment, style = MaterialTheme.typography.bodyMedium)

            val dateString = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                .format(Date(review.timestamp))

            Text(
                text = dateString,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
