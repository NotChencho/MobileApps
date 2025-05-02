package es.uc3m.android.mobile_app.screens

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import es.uc3m.android.mobile_app.viewmodel.MyViewModel
import com.google.firebase.firestore.DocumentId

@Composable
fun RestaurantDetailsScreen(
    navController: NavHostController,
    viewModel: MyViewModel = viewModel(),
    restaurantId: String? = null
) {
    val selectedRestaurant by viewModel.selectedRestaurant.collectAsState()

    // Track loading state
    var isLoading by remember { mutableStateOf(true) }

    // If we have a restaurant ID from navigation, load that specific restaurant
    LaunchedEffect(restaurantId) {
        if (restaurantId != null && restaurantId.isNotEmpty()) {
            isLoading = true
            viewModel.getRestaurantById(restaurantId)
        } else {
            // If no ID but we already have a selected restaurant, just use that
            isLoading = selectedRestaurant == null
        }
    }

    // Watch for changes in selectedRestaurant to update loading state
    LaunchedEffect(selectedRestaurant) {
        if (selectedRestaurant != null) {
            isLoading = false
        }
    }

    // If we're still loading, show loading state
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Once we have the restaurant data, display it
    selectedRestaurant?.let { restaurant ->
        // Top-level column for the entire screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Header Image or Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.LightGray)
            ) {
                // In a real app, use an image loading library like Coil to load the image from the URL
                // AsyncImage(
                //   model = restaurant.imageUrl,
                //   contentDescription = null,
                //   modifier = Modifier.fillMaxSize(),
                //   contentScale = ContentScale.Crop
                // )
            }

            // 2. Restaurant Title & Subtitle
            Text(
                text = restaurant.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Text(
                text = restaurant.cuisine,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // 3. Action Row (Add to itinerary + distance)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { /* Handle "Add to itinerary" */ }) {
                    Text("Add to my itinerary")
                }
                Text(
                    text = restaurant.distance,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 4. Price Range Information
            Text(
                text = "Price Based On Reviews ${restaurant.priceRange}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 5. Tabs (Recommended vs Other)
            var selectedTabIndex by remember { mutableStateOf(0) }
            val tabs = listOf("Recommended", "Other")

            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            // 6. List of Dishes based on selected tab

            val recommendedDishes = restaurant.dishes.filter { it.isRecommended == true }
            val otherDishes = restaurant.dishes.filter { it.isRecommended != true }

            val dishesToShow = if (selectedTabIndex == 0) recommendedDishes else otherDishes

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Let the list take up remaining space
            ) {
                items(dishesToShow) { dish ->
                    DishItem(dish)
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            // 7. Button: "View Similar Nearby Restaurants"
            Button(
                onClick = { /* Navigate to similar restaurants screen */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = "View Similar Nearby Restaurants")
            }
        }
    } ?: run {
        // If somehow selectedRestaurant is still null but we're not loading
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Restaurant not found")
        }
    }
}

// Single dish item layout
@Composable
fun DishItem(dish: Dish) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Dish image placeholder
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray)
        ) {
            // In a real app, use Coil or another image loading library:
            // AsyncImage(
            //   model = dish.imageUrl,
            //   contentDescription = dish.name,
            //   modifier = Modifier.fillMaxSize(),
            //   contentScale = ContentScale.Crop
            // )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Dish info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = dish.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Display star rating using repeated stars
            Text(text = "⭐".repeat(dish.rating))
            // Display price
            Text(
                text = "€${dish.price}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

data class Restaurant(
    @DocumentId val id: String = "", // This will automatically map to the Firestore document ID
    val name: String = "",
    val cuisine: String = "",
    val imageUrl: String = "",
    val priceRange: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val distance: String = "",
    val rating: Double = 0.0,
    val dishes: List<Dish> = emptyList()
)

data class GeoPoint(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class Dish(
    val name: String = "",
    val rating: Int = 0,
    val imageUrl: String = "",
    val price: Double = 0.0,
    val isRecommended: Boolean? = false
)