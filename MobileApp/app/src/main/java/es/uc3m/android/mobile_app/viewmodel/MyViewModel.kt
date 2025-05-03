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
            // Clear user preferences when logged out
            _userPreferences.value = null
        } else {
            // Load user preferences when logged in
            loadUserPreferences()
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
        _user.value = auth.currentUser
        _isUserLoggedIn.value = auth.currentUser != null
        if (_isUserLoggedIn.value) {
            loadUserPreferences()
        }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    // --- Login ---
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

    // --- Sign Up ---
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

    // --- Logout ---
    fun logout() {
        viewModelScope.launch {
            try {
                auth.signOut()
            } catch (e: Exception) {
                println("Error signing out: ${e.message}")
            }
        }
    }

    // --- Reset Auth Result ---
    fun resetAuthResult() {
        _authResult.value = AuthResult.Idle
    }

    // --- Load User Preferences ---
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

                        // Apply filters after loading preferences
                        applyFiltersToRestaurants()
                    } else {
                        // No preferences found, set default values
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

    // --- Save User Preferences ---
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

                    // Apply filters after saving new preferences
                    applyFiltersToRestaurants()
                } catch (e: Exception) {
                    _savePreferencesStatus.value = SaveStatus.Error(e.message ?: "Failed to save preferences")
                }
            }
        } else {
            _savePreferencesStatus.value = SaveStatus.Error("User not logged in")
        }
    }

    // --- Load Reviews from Firestore ---
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

    // --- Load Restaurants from Firestore ---
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

                            // Apply filters after loading restaurants
                            applyFiltersToRestaurants()
                        }
                    }
            } catch (e: Exception) {
                _restaurants.value = DataState.Error(e.message ?: "Exception loading restaurants")
            }
        }
    }

    // --- Apply Filters to Restaurants ---
    private fun applyFiltersToRestaurants() {
        val currentRestaurants = _restaurants.value
        val preferences = _userPreferences.value

        if (currentRestaurants is DataState.Success && preferences != null) {
            val allRestaurants = currentRestaurants.data

            // Apply filters based on preferences
            val filtered = allRestaurants.filter { restaurant ->
                // Filter by food type (cuisine)
                val matchesFoodType = restaurant.cuisine.equals(preferences.foodType, ignoreCase = true)

                // Filter by price range
                val matchesPriceRange = restaurant.priceRange.equals(preferences.priceRange, ignoreCase = true)

                // Return restaurants that match both criteria
                matchesFoodType && matchesPriceRange
            }

            _filteredRestaurants.value = DataState.Success(filtered)
        } else {
            // If there are no preferences or restaurants, just pass through the original data
            _filteredRestaurants.value = currentRestaurants
        }
    }

    // --- Get Restaurant by ID ---
    fun getRestaurantById(restaurantId: String) {
        viewModelScope.launch {
            try {
                val documentSnapshot = db.collection("restaurants").document(restaurantId).get().await()
                documentSnapshot?.let {
                    // Convert Firestore document to Restaurant object
                    val restaurant = it.toObject(Restaurant::class.java)
                    _selectedRestaurant.value = restaurant
                }
            } catch (e: Exception) {
                println("Error fetching restaurant: ${e.message}")
            }
        }
    }

    // --- Set Selected Restaurant ---
    fun setSelectedRestaurant(restaurant: Restaurant) {
        _selectedRestaurant.value = restaurant
    }

    // --- Helper function to parse restaurant documents ---
    private fun parseRestaurantsFromFirestore(snapshot: QuerySnapshot): List<Restaurant> {
        return snapshot.documents.mapNotNull { doc ->
            try {
                // Map the basic restaurant fields
                val restaurant = doc.toObject(Restaurant::class.java) ?: return@mapNotNull null
                restaurant
            } catch (e: Exception) {
                println("Error parsing restaurant document ${doc.id}: ${e.message}")
                null
            }
        }
    }
}