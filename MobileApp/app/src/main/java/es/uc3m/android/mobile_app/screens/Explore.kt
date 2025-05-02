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
        // Google Maps with Rounded Corners
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.LightGray)
        ) {
            GoogleMapWidget(restaurantsState)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display restaurants or friends activity
        when (restaurantsState) {
            is DataState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is DataState.Error -> {
                val errorMessage = (restaurantsState as DataState.Error).message
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: $errorMessage")
                }
            }

            is DataState.Success -> {
                val restaurants = (restaurantsState as DataState.Success<List<Restaurant>>).data

                if (restaurants.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No restaurants found")
                    }
                } else {
                    // Restaurant list
                    Text(
                        text = "Nearby Restaurants",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(restaurants) { restaurant ->
                            RestaurantItem(
                                restaurant = restaurant,
                                onClick = {
                                    // Set the selected restaurant in the ViewModel
                                    viewModel.setSelectedRestaurant(restaurant)
                                    // Navigate to details screen WITH the restaurant ID
                                    navController.navigate(NavGraph.RestaurantDetails.createRoute(restaurant.id))
                                }
                            )
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }

        // We can still show friends activity at the bottom if needed
        FriendsActivitySection(navController = navController)
    }
}

// Google Maps Widget that shows restaurant markers
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
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // Draw the default marker for Madrid
        Marker(
            state = MarkerState(position = madrid),
            title = "Madrid",
            snippet = "Capital of Spain"
        )

        // If we have restaurant data, draw markers for each restaurant
        if (restaurantsState is DataState.Success) {
            val restaurants = (restaurantsState as DataState.Success<List<Restaurant>>).data

            restaurants.forEach { restaurant ->
                // Create a LatLng from the restaurant's location
                val position = LatLng(
                    restaurant.location.latitude,
                    restaurant.location.longitude
                )

                // Add a marker for this restaurant
                Marker(
                    state = MarkerState(position = position),
                    title = restaurant.name,
                    snippet = restaurant.cuisine
                )
            }
        }
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
        // Restaurant image placeholder
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray)
        ) {
            // In a real app, use an image loading library like Coil
            // AsyncImage(
            //   model = restaurant.imageUrl,
            //   contentDescription = restaurant.name,
            //   modifier = Modifier.fillMaxSize(),
            //   contentScale = ContentScale.Crop
            // )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = restaurant.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = restaurant.cuisine,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⭐ ${restaurant.rating}",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = restaurant.priceRange,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = restaurant.distance,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun FriendsActivitySection(navController: NavHostController) {

    val friendsActivity = listOf(
        "David" to "Restaurant 1",
        "Ana" to "Restaurant 2",
        "Mario" to "Restaurant 3"
    )

    // Title
    Text(
        text = "Friends Recent Activity",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )

    // You can use a LazyColumn for better performance if the list grows
    Column {
        friendsActivity.forEach { (friendName, restaurantName) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Either navigate with a default ID or handle appropriately
                        navController.navigate(NavGraph.RestaurantDetails.createRoute("s8ZtXxCwTlwKCt8Za1JL"))
                    }

                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Friend icon or avatar placeholder
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

@Preview(showBackground = true)
@Composable
fun ExploreScreenPreview() {
    MyAppTheme {
        ExploreScreen(rememberNavController())
    }
}