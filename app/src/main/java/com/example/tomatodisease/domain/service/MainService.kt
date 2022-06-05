package com.example.tomatodisease.domain.service

import com.example.tomatodisease.domain.model.*
import kotlinx.coroutines.flow.Flow

interface MainService {
    suspend fun uploadSingleImage(image: RequestImage): ResponsePrediction

    suspend fun uploadMultipleImage(images: RequestMultipleImage): ResponseMultiplePredictions
}