package com.example.tomatodisease.domain.service

import com.example.tomatodisease.domain.model.RequestImage
import com.example.tomatodisease.domain.model.Response
import com.example.tomatodisease.domain.model.ResponsePredictionSocket
import com.example.tomatodisease.domain.model.SendResponse
import kotlinx.coroutines.flow.Flow

interface SocketService {

    suspend fun initSocket(): Response<Unit>

    fun observeSocket(): Flow<ResponsePredictionSocket>

    suspend fun sendImage(image: RequestImage)

    suspend fun closeSocket()
}