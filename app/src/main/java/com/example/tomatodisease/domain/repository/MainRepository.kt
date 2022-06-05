package com.example.tomatodisease.domain.repository

import com.example.tomatodisease.domain.model.*
import kotlinx.coroutines.flow.Flow

interface MainRepository {

    suspend fun uploadSingleImage(image: RequestImage): Flow<Response<ResponsePrediction>>

    suspend fun uploadMultipleImage(images: RequestMultipleImage): Flow<Response<ResponseMultiplePredictions>>
}