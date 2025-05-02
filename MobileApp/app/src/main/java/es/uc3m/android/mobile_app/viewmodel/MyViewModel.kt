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

    // --- Selected Restaurant State ---
    private val _selectedRestaurant = MutableStateFlow<Restaurant?>(null)
    val selectedRestaurant: StateFlow<Restaurant?> = _selectedRestaurant

    // --- Auth State Listener ---
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val firebaseUser = firebaseAuth.currentUser
        _user.value = firebaseUser
        _isUserLoggedIn.value = firebaseUser != null
        if (firebaseUser == null) {
            _authResult.value = AuthResult.Idle
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
        _user.value = auth.currentUser
        _isUserLoggedIn.value = auth.currentUser != null
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
                        }
                    }
            } catch (e: Exception) {
                _restaurants.value = DataState.Error(e.message ?: "Exception loading restaurants")
            }
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

                // If any manual mapping is needed, do it here
                // For example, if dishes are stored in a subcollection, you might need to fetch them separately

                restaurant
            } catch (e: Exception) {
                println("Error parsing restaurant document ${doc.id}: ${e.message}")
                null
            }
        }
    }
}