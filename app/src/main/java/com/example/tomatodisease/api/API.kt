package com.example.tomatodisease.api

import com.example.tomatodisease.domain.model.RequestImage
import com.example.tomatodisease.domain.model.RequestMultipleImage
import com.example.tomatodisease.domain.model.ResponseMultiplePredictions
import com.example.tomatodisease.domain.model.ResponsePrediction
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface API {

    companion object {
        //TODO: configure this value
        const val BASE_URL = "3747-158-140-162-149.ap.ngrok.io"
    }

    sealed class Endpoints(val url: String) {
        object StillImages: Endpoints("https://$BASE_URL")
        object Socket: Endpoints("ws://$BASE_URL/ws/live_detection")
    }

    @POST("still_images_base64")
    suspend fun uploadMultipleImages(
        @Body request: RequestMultipleImage
    ): Response<ResponseMultiplePredictions>

    @POST("still_image_base64")
    suspend fun uploadSingleImage(
        @Body request: RequestImage
    ): Response<ResponsePrediction>

}