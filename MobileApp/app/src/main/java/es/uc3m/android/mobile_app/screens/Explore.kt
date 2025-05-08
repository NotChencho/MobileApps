package es.uc3m.android.mobile_app.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import es.uc3m.android.mobile_app.NavGraph
import es.uc3m.android.mobile_app.R
import es.uc3m.android.mobile_app.screens.Restaurant
import es.uc3m.android.mobile_app.ui.theme.MyAppTheme
import es.uc3m.android.mobile_app.viewmodel.DataState
import es.uc3m.android.mobile_app.viewmodel.MyViewModel
import es.uc3m.android.mobile_app.viewmodel.UserPreferences
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import es.uc3m.android.mobile_app.viewmodel.Review
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp


@Composable
fun ExploreScreen(
    navController: NavHostController,
    viewModel: MyViewModel = viewModel()
) {
    // Track if map is expanded (0 = normal, 1 = half screen, 2 = full screen)
    var mapExpandState by remember { mutableStateOf(0) }

    // Load restaurants when the screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.loadRestaurants()
        viewModel.loadUserPreferences()
        viewModel.getLatestReviewsFromFollowing()
    }

    // Observe restaurants state (filtered ones)
    val filteredRestaurantsState by viewModel.filteredRestaurants.collectAsState()

    // Observe user preferences
    val userPreferences by viewModel.userPreferences.collectAsState()

    // Observe all restaurants (for map display)
    val allRestaurantsState by viewModel.restaurants.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Section 1: Google Maps (Can take variable height depending on expansion state)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .let {
                    when (mapExpandState) {
                        0 -> it.weight(1f) // Normal 1/3 view
                        1 -> it.weight(1.5f) // Half screen view
                        2 -> it.fillMaxSize() // Full screen view
                        else -> it.weight(1f)
                    }
                }
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.LightGray)
        ) {
            GoogleMapWidget(filteredRestaurantsState)

            // Control buttons for map size
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                // If in full screen mode, add "Back" button
                if (mapExpandState == 2) {
                    IconButton(
                        onClick = { mapExpandState = 0 }, // Return to normal view
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Minimize Map",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    // Expand/collapse button cycles through states
                    IconButton(
                        onClick = {
                            // Cycle through states: 0 -> 1 -> 2 -> 0
                            mapExpandState = (mapExpandState + 1) % 3
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Expand Map",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Active filters display - only show when not in full screen mode
        if (mapExpandState != 2) {
            userPreferences?.let { prefs ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Active Filters",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = true,
                                onClick = {
                                    // Navigate to settings
                                    navController.navigate(NavGraph.Settings.createRoute("From Explore"))
                                },
                                label = { Text("Food: ${prefs.foodType}") }
                            )
                            FilterChip(
                                selected = true,
                                onClick = {
                                    // Navigate to settings
                                    navController.navigate(NavGraph.Settings.createRoute("From Explore"))
                                },
                                label = { Text("Price: ${prefs.priceRange}") }
                            )
                        }
                    }
                }
            }
        }

        // Container for Section 2 & 3 (Takes the remaining height) - only show when not in full screen mode
        if (mapExpandState != 2) {
            Column(modifier = Modifier.weight(if (mapExpandState == 1) 1.5f else 2f)) {
                when (filteredRestaurantsState) {
                    is DataState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is DataState.Error -> {
                        val errorMessage = (filteredRestaurantsState as DataState.Error).message
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Error: $errorMessage")
                        }
                    }

                    is DataState.Success -> {
                        val restaurants = (filteredRestaurantsState as DataState.Success<List<Restaurant>>).data

                        if (restaurants.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("No restaurants match your preferences")
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = {
                                        navController.navigate(NavGraph.Settings.createRoute("From Empty Results"))
                                    }) {
                                        Text("Update Preferences")
                                    }
                                }
                            }
                        } else {
                            Column(modifier = Modifier.weight(1f)) { // Takes 1/2 of its parent's height
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Restaurants For You",
                                        style = MaterialTheme.typography.titleMedium,
                                    )

                                    TextButton(onClick = {
                                        navController.navigate(NavGraph.Settings.createRoute("From Restaurant List"))
                                    }) {
                                        Text("Change")
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize() // Fill the space provided by the weighted parent Column
                                ) {
                                    items(restaurants) { restaurant ->
                                        RestaurantItem(
                                            restaurant = restaurant,
                                            onClick = {
                                                viewModel.setSelectedRestaurant(restaurant)
                                                navController.navigate(NavGraph.RestaurantDetails.createRoute(restaurant.id))
                                            }
                                        )
                                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            }

                            // Section 3: Friends Activity (Takes 1/2 of the remaining container)
                            Column(modifier = Modifier.weight(1f)) {
                                FriendsActivitySectionContent(navController = navController)
                            }
                        }
                    }
                }
            }
        } // Close if (mapExpandState != 2)
    }
}

@Composable
fun GoogleMapWidget(
    restaurantsState: DataState<List<Restaurant>>
) {
    // Default to Madrid, Spain
    val madrid = LatLng(40.4168, -3.7038)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(madrid, 12f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(), // Fill the parent Box
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = MarkerState(position = madrid),
            title = "Madrid",
            snippet = "Capital of Spain"
        )

        if (restaurantsState is DataState.Success) {
            val restaurants = (restaurantsState as DataState.Success<List<Restaurant>>).data
            restaurants.forEach { restaurant ->
                val position = LatLng(
                    restaurant.location.latitude,
                    restaurant.location.longitude
                )
                Marker(
                    state = MarkerState(position = position),
                    title = restaurant.name,
                    snippet = restaurant.cuisine
                )
            }
        }
    }
}

// Restaurant Item Composable (No changes needed here)
@Composable
fun RestaurantItem(
    restaurant: Restaurant, // Assuming Restaurant has an imageUrl property
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        // Box for sizing and clipping the image
        Box(
            modifier = Modifier
                .size(80.dp) // Define the size of the image area
                .clip(RoundedCornerShape(8.dp))
                // Add a background color to the Box itself in case the placeholder/error is transparent
                // or if the image loading takes time.
                .background(Color.LightGray)
        ) {
            // Use AsyncImage to load the restaurant photo
            AsyncImage(
                model = restaurant.imageUrl, // The URL of the restaurant's image
                contentDescription = restaurant.name, // Accessibility: Describe the image
                modifier = Modifier.fillMaxSize(), // Make the image fill the Box
                contentScale = ContentScale.Crop, // Crop the image to fit the bounds
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = restaurant.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = restaurant.cuisine, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⭐ ${restaurant.rating}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = restaurant.priceRange, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = restaurant.distance, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun FriendsActivitySectionContent(
    navController: NavHostController,
    viewModel: MyViewModel = viewModel()
) {
    val recentFriendReviews by viewModel.recentReviewsFromFollowed.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Recent Friend Activity",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (recentFriendReviews.isEmpty()) {
            Text(text = "No recent activity from followed users.")
        } else {
            LazyColumn {
                items(recentFriendReviews) { review ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                navController.navigate("public_profile/${review.user}")
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Review photo display
                                if (!review.photoUrl.isNullOrEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.LightGray)
                                    ) {
                                        AsyncImage(
                                            model = review.photoUrl,
                                            contentDescription = "Review photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                Column {
                                    Text(text = "${review.user} reviewed ${review.dish} at ${review.restaurant}")
                                    Text(
                                        text = review.comment,
                                        style = MaterialTheme.typography.bodySmall,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExploreScreenPreview() {
    MyAppTheme {
        ExploreScreen(rememberNavController())
    }
}