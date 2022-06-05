package com.example.tomatodisease.domain.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResponsePrediction(
    // Name of detected object
    @SerializedName("class_name")
    val className: String,

    // Confidence of detected object
    @SerializedName("confidence")
    val confidence: String,
)

@Serializable
data class SocketResponse(
    @SerialName("class_name")
    val className: String = "",
    @SerialName("confidence")
    val confidence: String = "",
    val error: String? = null
)

@Serializable
data class ResponsePredictionSocket(
    @SerialName("class_name")
    val className: String = "",
    val confidence: Float = 0F,
    val error: String? = null
)

@Serializable
data class ResponseMultiplePredictions(
    val result: List<ResponsePredictionIndexed>
)

@Serializable
data class ResponsePredictionIndexed(
    val index: Int,
    @SerializedName("class_name")
    val className: String,
    @SerializedName("confidence")
    val confidence: String
)

data class SendResponse(
    val error: String? = null
)

sealed class Response<T> {
    class Success<T>(val data: T) : Response<T>()

    class Loading<T> : Response<T>()

    class Error<T>(val exception: Exception) : Response<T>()

    class ApiError<T>(val message: String, val code: Int) : Response<T>()

    class NetworkError<T>(val throwable: Throwable) : Response<T>()

    class NullResponse<T> : Response<T>()
}

