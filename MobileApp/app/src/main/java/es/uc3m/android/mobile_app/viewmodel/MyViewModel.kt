package es.uc3m.android.mobile_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import es.uc3m.android.mobile_app.screens.Restaurant
import es.uc3m.android.mobile_app.screens.GeoPoint
import es.uc3m.android.mobile_app.screens.Dish

// --- Auth Result Sealed Class ---
sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
    object Idle : AuthResult()
}

// --- Data Loading State ---
sealed class DataState<out T> {
    object Loading : DataState<Nothing>()
    data class Success<T>(val data: T) : DataState<T>()
    data class Error(val message: String) : DataState<Nothing>()
}

// --- Save Preferences Status ---
sealed class SaveStatus {
    object Idle : SaveStatus()
    object Loading : SaveStatus()
    object Success : SaveStatus()
    data class Error(val message: String) : SaveStatus()
}

// --- User Preferences Data Class ---
data class UserPreferences(
    val foodType: String = "",
    val priceRange: String = "",
    val allergyPreference: String = "",
    val otherPreference: String = "",
    val date: String = ""
)

class MyViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val followCollection = db.collection("follows") // <-- NUEVO

    // --- Authentication State ---
    private val _user = MutableStateFlow<FirebaseUser?>(null)
    val user: StateFlow<FirebaseUser?> = _user

    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn

    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult

    // --- Reviews State ---
    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews

    // --- Restaurants State ---
    private val _restaurants = MutableStateFlow<DataState<List<Restaurant>>>(DataState.Loading)
    val restaurants: StateFlow<DataState<List<Restaurant>>> = _restaurants

    // --- Filtered Restaurants State ---
    private val _filteredRestaurants = MutableStateFlow<DataState<List<Restaurant>>>(DataState.Loading)
    val filteredRestaurants: StateFlow<DataState<List<Restaurant>>> = _filteredRestaurants

    // --- Selected Restaurant State ---
    private val _selectedRestaurant = MutableStateFlow<Restaurant?>(null)
    val selectedRestaurant: StateFlow<Restaurant?> = _selectedRestaurant

    // --- User Preferences State ---
    private val _userPreferences = MutableStateFlow<UserPreferences?>(null)
    val userPreferences: StateFlow<UserPreferences?> = _userPreferences

    // --- Save Preferences Status ---
    private val _savePreferencesStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val savePreferencesStatus: StateFlow<SaveStatus> = _savePreferencesStatus

    // --- Auth State Listener ---
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val firebaseUser = firebaseAuth.currentUser
        _user.value = firebaseUser
        _isUserLoggedIn.value = firebaseUser != null
        if (firebaseUser == null) {
            _authResult.value = AuthResult.Idle
            _userPreferences.value = null
        } else {
            loadUserPreferences()
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
        _user.value = auth.currentUser
        _isUserLoggedIn.value = auth.currentUser != null
        if (_isUserLoggedIn.value) {
            loadUserPreferences()
            loadReviews()
        }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _authResult.value = AuthResult.Success
                loadUserPreferences()
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error(e.message ?: "Login failed")
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                _authResult.value = AuthResult.Success
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error(e.message ?: "Sign up failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                auth.signOut()
            } catch (e: Exception) {
                println("Error signing out: ${e.message}")
            }
        }
    }

    fun resetAuthResult() {
        _authResult.value = AuthResult.Idle
    }

    fun loadUserPreferences() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    val userId = currentUser.uid
                    val document = db.collection("userPreferences").document(userId).get().await()

                    if (document.exists()) {
                        val preferences = document.toObject(UserPreferences::class.java)
                        _userPreferences.value = preferences
                        applyFiltersToRestaurants()
                    } else {
                        _userPreferences.value = UserPreferences(
                            foodType = "Italian",
                            priceRange = "$",
                            allergyPreference = "None",
                            otherPreference = "Outdoor Seating",
                            date = ""
                        )
                    }
                } catch (e: Exception) {
                    println("Error loading user preferences: ${e.message}")
                }
            }
        }
    }

    fun saveUserPreferences(preferences: UserPreferences) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                _savePreferencesStatus.value = SaveStatus.Loading
                try {
                    val userId = currentUser.uid
                    db.collection("userPreferences").document(userId).set(preferences).await()
                    _userPreferences.value = preferences
                    _savePreferencesStatus.value = SaveStatus.Success
                    applyFiltersToRestaurants()
                } catch (e: Exception) {
                    _savePreferencesStatus.value = SaveStatus.Error(e.message ?: "Failed to save preferences")
                }
            }
        } else {
            _savePreferencesStatus.value = SaveStatus.Error("User not logged in")
        }
    }

    fun loadReviews() {
        viewModelScope.launch {
            try {
                db.collection("reviews")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null || snapshot == null) {
                            println("Error fetching reviews: ${e?.message}")
                            return@addSnapshotListener
                        }
                        val reviewList = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Review::class.java)?.apply { id = doc.id }
                        }
                        _reviews.value = reviewList
                    }
            } catch (e: Exception) {
                println("Exception: ${e.message}")
            }
        }
    }

    fun loadRestaurants() {
        viewModelScope.launch {
            _restaurants.value = DataState.Loading
            try {
                db.collection("restaurants")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            _restaurants.value = DataState.Error(e.message ?: "Error loading restaurants")
                            return@addSnapshotListener
                        }

                        snapshot?.let {
                            val restaurantList = parseRestaurantsFromFirestore(it)
                            _restaurants.value = DataState.Success(restaurantList)
                            applyFiltersToRestaurants()
                        }
                    }
            } catch (e: Exception) {
                _restaurants.value = DataState.Error(e.message ?: "Exception loading restaurants")
            }
        }
    }

    private fun applyFiltersToRestaurants() {
        val currentRestaurants = _restaurants.value
        val preferences = _userPreferences.value

        if (currentRestaurants is DataState.Success && preferences != null) {
            val allRestaurants = currentRestaurants.data
            val filtered = allRestaurants.filter { restaurant ->
                val matchesFoodType = restaurant.cuisine.equals(preferences.foodType, ignoreCase = true)
                val matchesPriceRange = restaurant.priceRange.equals(preferences.priceRange, ignoreCase = true)
                matchesFoodType && matchesPriceRange
            }
            _filteredRestaurants.value = DataState.Success(filtered)
        } else {
            _filteredRestaurants.value = currentRestaurants
        }
    }

    fun getRestaurantById(restaurantId: String) {
        viewModelScope.launch {
            try {
                val documentSnapshot = db.collection("restaurants").document(restaurantId).get().await()
                documentSnapshot?.let {
                    val restaurant = it.toObject(Restaurant::class.java)
                    _selectedRestaurant.value = restaurant
                }
            } catch (e: Exception) {
                println("Error fetching restaurant: ${e.message}")
            }
        }
    }

    fun setSelectedRestaurant(restaurant: Restaurant) {
        _selectedRestaurant.value = restaurant
    }

    private fun parseRestaurantsFromFirestore(snapshot: QuerySnapshot): List<Restaurant> {
        return snapshot.documents.mapNotNull { doc ->
            try {
                val restaurant = doc.toObject(Restaurant::class.java) ?: return@mapNotNull null
                restaurant
            } catch (e: Exception) {
                println("Error parsing restaurant document ${doc.id}: ${e.message}")
                null
            }
        }
    }

    fun addDishReview(review: Review, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    val reviewRef = db.collection("reviews").document()
                    reviewRef.set(review).await()
                    val updatedReviews = _reviews.value.toMutableList()
                    updatedReviews.add(review.copy(id = reviewRef.id))
                    _reviews.value = updatedReviews
                    onSuccess()
                } catch (e: Exception) {
                    onError(e.message ?: "Failed to add review")
                }
            }
        } else {
            onError("User not logged in")
        }
    }

    fun getReviewsForDish(restaurantName: String, dishName: String): List<Review> {
        return _reviews.value.filter {
            it.restaurant == restaurantName && it.dish == dishName
        }.sortedByDescending { it.timestamp }
    }

    fun getUserReviews(): List<Review> {
        val userEmail = auth.currentUser?.email ?: return emptyList()
        return _reviews.value.filter { it.user == userEmail }
    }

    fun deleteReview(reviewId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("reviews").document(reviewId).delete().await()
                val updatedReviews = _reviews.value.filter { it.id != reviewId }
                _reviews.value = updatedReviews
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to delete review")
            }
        }
    }

    // --- FOLLOW SYSTEM ---
    fun followUser(follower: String, followed: String) {
        val followData = mapOf(
            "follower" to follower,
            "followed" to followed,
            "timestamp" to System.currentTimeMillis()
        )
        followCollection.add(followData)
            .addOnSuccessListener { println("Followed $followed") }
            .addOnFailureListener { println("Error following user: ${it.message}") }
    }

    fun unfollowUser(follower: String, followed: String) {
        followCollection
            .whereEqualTo("follower", follower)
            .whereEqualTo("followed", followed)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
            }
            .addOnFailureListener { println("Error unfollowing user: ${it.message}") }
    }

    fun getFollowers(userEmail: String, callback: (List<String>) -> Unit) {
        followCollection
            .whereEqualTo("followed", userEmail)
            .get()
            .addOnSuccessListener { snapshot ->
                val followers = snapshot.documents.mapNotNull { it.getString("follower") }
                callback(followers)
            }
            .addOnFailureListener { println("Error fetching followers: ${it.message}") }
    }

    fun getFollowing(userEmail: String, callback: (List<String>) -> Unit) {
        followCollection
            .whereEqualTo("follower", userEmail)
            .get()
            .addOnSuccessListener { snapshot ->
                val following = snapshot.documents.mapNotNull { it.getString("followed") }
                callback(following)
            }
            .addOnFailureListener { println("Error fetching following: ${it.message}") }
    }

    fun isFollowing(follower: String, followed: String, callback: (Boolean) -> Unit) {
        followCollection
            .whereEqualTo("follower", follower)
            .whereEqualTo("followed", followed)
            .get()
            .addOnSuccessListener { snapshot ->
                callback(!snapshot.isEmpty)
            }
            .addOnFailureListener { println("Error checking following status: ${it.message}") }
    }
}
