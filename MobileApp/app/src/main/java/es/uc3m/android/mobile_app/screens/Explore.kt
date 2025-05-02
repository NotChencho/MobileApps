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

@Composable
fun ExploreScreen(
    navController: NavHostController,
    viewModel: MyViewModel = viewModel()
) {
    // Load restaurants when the screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.loadRestaurants()
    }

    // Observe restaurants state
    val restaurantsState by viewModel.restaurants.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Section 1: Google Maps (Takes 1/3 of total height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Assign 1/3 of the total available height
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.LightGray)
        ) {
            GoogleMapWidget(restaurantsState)
        }

        // Container for Section 2 & 3 (Takes the remaining 2/3 height)
        Column(modifier = Modifier.weight(2f)) {
            when (restaurantsState) {
                is DataState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(), // Fill the 2/3 parent
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is DataState.Error -> {
                    val errorMessage = (restaurantsState as DataState.Error).message
                    Box(
                        modifier = Modifier.fillMaxSize(), // Fill the 2/3 parent
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Error: $errorMessage")
                    }
                }

                is DataState.Success -> {
                    val restaurants = (restaurantsState as DataState.Success<List<Restaurant>>).data

                    if (restaurants.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(), // Fill the 2/3 parent
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No restaurants found")
                        }
                    } else {
                        // Section 2: Restaurant List (Takes 1/2 of the 2/3 container = 1/3 of total)
                        Column(modifier = Modifier.weight(1f)) { // Takes 1/2 of its parent's height
                            Text(
                                text = "Nearby Restaurants",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

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

                        // Section 3: Friends Activity (Takes 1/2 of the 2/3 container = 1/3 of total)
                        // Call the composable that provides the content for the friends activity section
                        Column(modifier = Modifier.weight(1f)) { // Takes 1/2 of its parent's height
                            FriendsActivitySectionContent(navController = navController) // Call the content composable
                        }
                    }
                }
            }
        }
    }
}

// Google Maps Widget (No changes needed here)
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
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray)
        ) {
            // Image placeholder
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


// Composable that provides the CONTENT for the Friends Activity section
@Composable
fun FriendsActivitySectionContent(navController: NavHostController) {
    val friendsActivity = listOf(
        "David" to "Restaurant 1",
        "Ana" to "Restaurant 2",
        "Mario" to "Restaurant 3",
        // Add more friends here to test scrolling
        "Carlos" to "Restaurant 4",
        "Elena" to "Restaurant 5",
        "Sergio" to "Restaurant 6",
        "Laura" to "Restaurant 7",
        "Javier" to "Restaurant 8",
        "Sofia" to "Restaurant 9",
        "Diego" to "Restaurant 10",
        "Isabel" to "Restaurant 11",
        "Miguel" to "Restaurant 12",
        "Carmen" to "Restaurant 13",
        "Pablo" to "Restaurant 14",
        "Lucia" to "Restaurant 15"
    )

    // This Column contains the title and the scrollable list.
    // It does NOT have a weight modifier here. Its size is determined by the parent Column in ExploreScreen.
    Column(modifier = Modifier.fillMaxSize()) { // Fill the space given by the parent weighted Column
        // Title
        Text(
            text = "Friends Recent Activity",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // LazyColumn for a scrollable list within this section
        LazyColumn(
            modifier = Modifier.fillMaxSize() // Fill the remaining space in THIS Column
        ) {
            items(friendsActivity) { (friendName, restaurantName) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Example navigation - ideally use a specific restaurant ID
                            navController.navigate(NavGraph.RestaurantDetails.createRoute("s8ZtXxCwTlwKCt8Za1JL"))
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(50)),
                        color = Color.LightGray
                    ) {
                        // Could place an Icon or initial inside
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = friendName, style = MaterialTheme.typography.bodyLarge)
                        Text(text = restaurantName, style = MaterialTheme.typography.bodyMedium)
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