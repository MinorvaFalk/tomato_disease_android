package com.example.tomatodisease.domain.service

import android.util.Log
import com.example.tomatodisease.api.API
import com.example.tomatodisease.di.NetworkModule
import com.example.tomatodisease.domain.model.RequestImage
import com.example.tomatodisease.domain.model.Response
import com.example.tomatodisease.domain.model.ResponsePrediction
import com.example.tomatodisease.domain.model.ResponsePredictionSocket
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.websocket.*
import io.ktor.websocket.serialization.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SocketServiceImpl @Inject constructor(
    @NetworkModule.SocketClient
    val socketClient: HttpClient
): SocketService {

    companion object {
        private const val TAG = "SocketServiceImpl"
    }

    private var socket: WebSocketSession? = null

    override suspend fun initSocket(): Response<Unit> {
        return try {
            socket = socketClient.webSocketSession{
                url(API.Endpoints.Socket.url)
            }

            if (socket?.isActive == true) {
                Response.Success(Unit)
            } else {
                Response.ApiError("Socket inactive", 400)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, e.message ?: e.toString())
            return Response.Error(e)
        }
    }

    override suspend fun sendImage(image: RequestImage) {
        try {
            socket?.send(Frame.Text(Json.encodeToString(image)))
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: e.toString())
            e.printStackTrace()
        }
    }


    override fun observeSocket(): Flow<ResponsePredictionSocket> =
        try {
            socket?.incoming
                ?.receiveAsFlow()
                ?.filter { it is Frame.Text }
                ?.map {
                    val json = (it as? Frame.Text)?.readText() ?: ""
                    Json.decodeFromString(json)
                } ?: flow {  }
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: e.toString())
            e.printStackTrace()
            flow { ResponsePredictionSocket(error = e.message ?: e.toString()) }
    }

    override suspend fun closeSocket() {
        socket?.close()
    }
}