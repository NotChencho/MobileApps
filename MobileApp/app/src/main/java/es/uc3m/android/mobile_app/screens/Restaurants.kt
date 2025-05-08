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
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext


@Composable
fun RestaurantDetailsScreen(
    navController: NavHostController,
    viewModel: MyViewModel = viewModel(),
    restaurantId: String? = null
) {
    val selectedRestaurant by viewModel.selectedRestaurant.collectAsState()
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

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

    LaunchedEffect(selectedRestaurant) {
        if (selectedRestaurant != null) {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    selectedRestaurant?.let { restaurant ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
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

            Text(
                text = "Price Based On Reviews ${restaurant.priceRange}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            restaurant.websiteLink?.let { url -> // Check if websiteLink is not null
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth() // Make the button fill the width
                ) {
                    Text("Visit Website")
                }
            }

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

            val recommendedDishes = restaurant.dishes.filter { it.isRecommended == true }
            val otherDishes = restaurant.dishes.filter { it.isRecommended != true }

            val dishesToShow = if (selectedTabIndex == 0) recommendedDishes else otherDishes

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(dishesToShow) { dish ->
                    DishItem(dish, restaurant.name, navController)
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

        }
    } ?: run {
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
            Text(text = "⭐".repeat(dish.rating))
            Text(
                text = "€${dish.price}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Column {

            Button(
                onClick = {
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
    @DocumentId val id: String = "",
    val name: String = "",
    val cuisine: String = "",
    val imageUrl: String = "",
    val priceRange: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val distance: String = "",
    val rating: Double = 0.0,
    val dishes: List<Dish> = emptyList(),
    val Allergies: List<String>? = null,
    val Other: List<String>? = null,
    val websiteLink: String? = "https://www.latagliatella.es/" // Added websiteLink field
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