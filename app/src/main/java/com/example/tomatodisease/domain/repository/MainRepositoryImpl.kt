package com.example.tomatodisease.domain.repository

import com.example.tomatodisease.di.DispatchersModule
import com.example.tomatodisease.domain.model.*
import com.example.tomatodisease.domain.service.MainService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    private val service: MainService,
    @DispatchersModule.IoDispatcher
    val dispatcher: CoroutineDispatcher
): MainRepository {
    override suspend fun uploadSingleImage(
        image: RequestImage
    ): Flow<Response<ResponsePrediction>> = flow {
        try {
            val response = service.uploadSingleImage(image)

            emit(Response.Success(response))
        } catch (e: Exception) {
            emit(Response.Error(e))
        }
    }
        .onStart { emit(Response.Loading()) }
        .flowOn(dispatcher)


    override suspend fun uploadMultipleImage(
        images: RequestMultipleImage
    ): Flow<Response<ResponseMultiplePredictions>> = flow {
        try {
            val response = service.uploadMultipleImage(images)

            emit(Response.Success(response))
        } catch (e: Exception) {
            emit(Response.Error(e))
        }
    }
        .onStart { emit(Response.Loading()) }
        .flowOn(dispatcher)

}