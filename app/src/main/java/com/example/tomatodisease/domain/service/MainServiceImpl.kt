package com.example.tomatodisease.domain.service

import com.example.tomatodisease.di.NetworkModule
import com.example.tomatodisease.domain.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.websocket.*
import io.ktor.websocket.serialization.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import java.nio.charset.Charset
import javax.inject.Inject

class MainServiceImpl @Inject constructor(
    @NetworkModule.HTTPClient
    val httpClient: HttpClient,
): MainService {

    override suspend fun uploadSingleImage(image: RequestImage): ResponsePrediction =
        httpClient.post("/still_image_base64") {
            contentType(ContentType.Application.Json)
            setBody(image)
        }
            .body()

    override suspend fun uploadMultipleImage(images: RequestMultipleImage): ResponseMultiplePredictions =
        httpClient.post("/still_images_base64") {
            contentType(ContentType.Application.Json)
            setBody(images)
        }
            .body()

}
