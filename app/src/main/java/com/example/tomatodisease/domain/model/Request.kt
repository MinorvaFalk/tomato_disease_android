package com.example.tomatodisease.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RequestMultipleImage(
    val images: List<RequestImageIndexed>
)

@Serializable
data class RequestImageIndexed(
    val index: Int,
    val imageString: String
)

@Serializable
data class RequestImage(
    // Image file encoded from Bitmap to base64
    val imageString: String
)