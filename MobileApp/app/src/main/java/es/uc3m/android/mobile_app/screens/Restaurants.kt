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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import es.uc3m.android.mobile_app.viewmodel.MyViewModel
import com.google.firebase.firestore.DocumentId
import androidx.compose.ui.layout.ContentScale // Import ContentScale
import androidx.compose.ui.res.painterResource // Import painterResource
import es.uc3m.android.mobile_app.R // Import your R class
import coil.compose.AsyncImage // Import AsyncImage


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

                AsyncImage(
                   model = restaurant.imageUrl,
                   contentDescription = null,
                   modifier = Modifier.fillMaxSize(),
                   contentScale = ContentScale.Crop
                )
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
                    DishItem(dish, restaurant.name, navController)
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                }
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

@Composable
fun DishItem(dish: Dish, restaurantName: String, navController: NavHostController) {
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
            AsyncImage(
               model = dish.imageUrl,
               contentDescription = dish.name,
               modifier = Modifier.fillMaxSize(),
               contentScale = ContentScale.Crop
             )
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

        // Review actions
        Column {

            Button(
                onClick = {
                    // Get the restaurant ID from the restaurant object
                    // or use an empty string if somehow not available
                    val restaurantIdToPass = ""
                    navController.navigate("dish_review/${restaurantIdToPass}/${restaurantName}/${dish.name}")
                },
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text("Review", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    navController.navigate("dish_reviews_list/${restaurantName}/${dish.name}")
                }
            ) {
                Text("See Reviews", fontSize = 12.sp)
            }
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
    val isRecommended: Boolean = true
)