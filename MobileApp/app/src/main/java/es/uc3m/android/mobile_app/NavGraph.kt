package es.uc3m.android.mobile_app

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.net.Uri


const val EXPLORE_ROUTE = "explore"
const val REVIEWS_ROUTE = "reviews"
const val LOGIN_ROUTE = "login"
const val SETTINGS_ROUTE = "settings"
const val REVIEW_DETAILS_ROUTE = "review_details"
const val SIGNUP_ROUTE = "signup"
const val RESTAURANT_DETAILS_ROUTE = "restaurant_details"
const val PROFILE_ROUTE = "profile"
const val RESTAURANTS_ROUTE = "restaurants"
const val DISH_REVIEW_ROUTE = "dish_review"
const val DISH_REVIEWS_LIST_ROUTE = "dish_reviews_list"
const val PUBLIC_PROFILE_ROUTE = "public_profile"

sealed class NavGraph(val route: String, val arguments: List<NamedNavArgument> = emptyList()) {

    data object Explore : NavGraph(EXPLORE_ROUTE)

    data object Reviews : NavGraph(REVIEWS_ROUTE)

    data object Login : NavGraph(LOGIN_ROUTE)
    data object SignUp : NavGraph(SIGNUP_ROUTE)
    data object Profile : NavGraph("profile")

    data object Settings : NavGraph("$SETTINGS_ROUTE/{source}",
        arguments = listOf(navArgument("source") { type = NavType.StringType })
    ) {
        fun createRoute(source: String) = "$SETTINGS_ROUTE/$source"
    }

    data object ReviewDetails : NavGraph("$REVIEW_DETAILS_ROUTE/{friend}/{restaurant}",
        arguments = listOf(
            navArgument("friend") { type = NavType.StringType },
            navArgument("restaurant") { type = NavType.StringType }
        )
    ) {
        fun createRoute(friend: String, restaurant: String) =
            "$REVIEW_DETAILS_ROUTE/$friend/$restaurant"
    }

    data object RestaurantDetails : NavGraph(
        route = "$RESTAURANT_DETAILS_ROUTE?restaurantId={restaurantId}",
        arguments = listOf(
            navArgument("restaurantId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) {
        fun createRoute(restaurantId: String? = null): String {
            return if (restaurantId != null) {
                "$RESTAURANT_DETAILS_ROUTE?restaurantId=$restaurantId"
            } else {
                RESTAURANT_DETAILS_ROUTE
            }
        }
    }

    data object DishReview : NavGraph(
        route = "$DISH_REVIEW_ROUTE/{restaurantId}/{restaurantName}/{dishName}",
        arguments = listOf(
            navArgument("restaurantId") {
                type = NavType.StringType
                defaultValue = "" // Add default value
            },
            navArgument("restaurantName") { type = NavType.StringType },
            navArgument("dishName") { type = NavType.StringType }
        )
    ) {
        fun createRoute(restaurantId: String = "", restaurantName: String, dishName: String) =
            "$DISH_REVIEW_ROUTE/$restaurantId/$restaurantName/$dishName"
    }

    data object DishReviewsList : NavGraph(
        route = "$DISH_REVIEWS_LIST_ROUTE/{restaurantName}/{dishName}",
        arguments = listOf(
            navArgument("restaurantName") { type = NavType.StringType },
            navArgument("dishName") { type = NavType.StringType }
        )
    ) {
        fun createRoute(restaurantName: String, dishName: String) =
            "$DISH_REVIEWS_LIST_ROUTE/$restaurantName/$dishName"
    }

    data object PublicProfile : NavGraph("public_profile/{email}") {
        fun createRoute(email: String): String = "public_profile/$email"
    }

    data object UserList : NavGraph("user_list/{title}/{emails}") {
        fun createRoute(title: String, emails: List<String>): String {
            val joined = emails.joinToString("|")
            return "user_list/$title/$joined"
        }
    }

}