package com.example.tomatodisease.domain.repository

import android.util.Log
import com.example.tomatodisease.api.API
import com.example.tomatodisease.di.DispatchersModule
import com.example.tomatodisease.domain.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class MainRepositoryRetrofit @Inject constructor(
    private val api: API,
    @DispatchersModule.IoDispatcher
    private val dispatcher: CoroutineDispatcher
): MainRepository {
    companion object {
        private const val TAG = "MainRepositoryRetrofit"
    }

    override suspend fun uploadSingleImage(image: RequestImage)
    : Flow<Response<ResponsePrediction>> =
        flow {
            try {
                val response = api.uploadSingleImage(image)
                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null) {
                        emit(Response.Success(body))
                    }else {
                        emit(Response.NullResponse())
                    }
                }

            }catch (e: Exception) {
                Log.e(TAG, e.message ?: e.toString())
                emit(Response.Error(e))
            }
        }
            .onStart { emit(Response.Loading()) }
            .flowOn(dispatcher)

    override suspend fun uploadMultipleImage(images: RequestMultipleImage)
    : Flow<Response<ResponseMultiplePredictions>> =
        flow {
            try {
                val response = api.uploadMultipleImages(images)
                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null) {
                        emit(Response.Success(body))
                    }else {
                        emit(Response.NullResponse())
                    }
                }else {
                    emit(Response.ApiError(response.message(), response.code()))
                }

            } catch (e: Exception) {
                emit(Response.Error(e))
            }
        }
            .onStart { emit(Response.Loading()) }
            .flowOn(dispatcher)


}