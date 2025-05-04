package es.uc3m.android.mobile_app.viewmodel

import com.google.firebase.firestore.Exclude

data class Review(
    @get:Exclude var id: String? = null,
    val user: String = "",
    val restaurant: String = "",
    val dish: String = "", // Added dish field
    val rating: Int = 0,
    val comment: String = "",
    val title: String = "", // Added title field
    val timestamp: Long = System.currentTimeMillis(), // For sorting reviews by recency
    val photoUrl: String? = null // URL to the uploaded photo in Firebase Storage
)