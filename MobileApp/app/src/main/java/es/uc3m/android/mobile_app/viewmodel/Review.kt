package es.uc3m.android.mobile_app.viewmodel

import com.google.firebase.firestore.Exclude

data class Review(
    @get:Exclude var id: String? = null,
    val user: String = "",
    val restaurant: String = "",
    val rating: Int = 0,
    val comment: String = ""
)
