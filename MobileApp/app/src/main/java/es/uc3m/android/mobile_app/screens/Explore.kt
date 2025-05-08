package es.uc3m.android.mobile_app.screens

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import es.uc3m.android.mobile_app.NavGraph
import es.uc3m.android.mobile_app.PermissionHandler
import es.uc3m.android.mobile_app.ui.theme.MyAppTheme
import es.uc3m.android.mobile_app.viewmodel.DataState
import es.uc3m.android.mobile_app.viewmodel.MyViewModel
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp

@Composable
fun ExploreScreen(
    navController: NavHostController,
    viewModel: MyViewModel = viewModel()
) {
    var mapExpandState by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadRestaurants()
        viewModel.loadUserPreferences()
        viewModel.getLatestReviewsFromFollowing()
    }

    val filteredRestaurantsState by viewModel.filteredRestaurants.collectAsState()
    val userPreferences by viewModel.userPreferences.collectAsState()

    val allRestaurantsState by viewModel.restaurants.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .let {
                    when (mapExpandState) {
                        0 -> it.weight(1f)
                        1 -> it.weight(1.5f)
                        2 -> it.fillMaxSize()
                        else -> it.weight(1f)
                    }
                }
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.LightGray)
        ) {
            GoogleMapWidget(filteredRestaurantsState)

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                if (mapExpandState == 2) {
                    IconButton(
                        onClick = { mapExpandState = 0 },
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
                    IconButton(
                        onClick = {
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
                                    navController.navigate(NavGraph.Settings.createRoute("From Explore"))
                                },
                                label = { Text("Food: ${prefs.foodType}") }
                            )
                            FilterChip(
                                selected = true,
                                onClick = {
                                    navController.navigate(NavGraph.Settings.createRoute("From Explore"))
                                },
                                label = { Text("Price: ${prefs.priceRange}") }
                            )
                        }
                    }
                }
            }
        }

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
                            Column(modifier = Modifier.weight(1f)) {
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
                                    modifier = Modifier.fillMaxSize()
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

                            Column(modifier = Modifier.weight(1f)) {
                                FriendsActivitySectionContent(navController = navController)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleMapWidget(
    restaurantsState: DataState<List<Restaurant>>
) {
    val madrid = LatLng(40.4168, -3.7038)
    val context = LocalContext.current

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasLocationPermission by remember { mutableStateOf(
        PermissionHandler.isLocationPermissionGranted(context)
    )}

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        hasLocationPermission = allGranted

        if (allGranted) {
            getUserLocation(context) { location ->
                userLocation = location
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            userLocation ?: madrid,
            12f
        )
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                PermissionHandler.getLocationPermissions().toTypedArray()
            )
        } else {
            getUserLocation(context) { location ->
                userLocation = location
            }
        }
    }

    LaunchedEffect(userLocation) {
        userLocation?.let { location ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 15f)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(), // Fill the parent Box
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission
            )
        ) {
            userLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = "Your Location",
                    snippet = "You are here"
                )
            }

            if (restaurantsState is DataState.Success) {
                val restaurants = (restaurantsState).data
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
}

@SuppressLint("MissingPermission")
private fun getUserLocation(context: Context, onLocationReceived: (LatLng) -> Unit) {
    // Check if we have permission
    if (!PermissionHandler.isLocationPermissionGranted(context)) {
        return
    }

    try {
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val userLatLng = LatLng(location.latitude, location.longitude)
                onLocationReceived(userLatLng)
            }
        }
    } catch (e: Exception) {
        println("Error getting location: ${e.message}")
    }
}

@Composable
fun RestaurantItem(
    restaurant: Restaurant,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp) // Define the size of the image area
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray)
        ) {
            AsyncImage(
                model = restaurant.imageUrl, // The URL
                contentDescription = restaurant.name, // Accessibility
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