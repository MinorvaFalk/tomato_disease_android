package com.example.tomatodisease.ui.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tomatodisease.domain.model.*
import com.example.tomatodisease.domain.repository.DataStoreRepository
import com.example.tomatodisease.domain.repository.MainRepository
import com.example.tomatodisease.domain.service.SocketService
import com.example.tomatodisease.utils.PreferencesKey
import com.example.tomatodisease.utils.UiEvent
import com.example.tomatodisease.utils.toBase64
import com.google.mlkit.vision.objects.DetectedObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataStore: DataStoreRepository,
    private val repository: MainRepository,
    private val socketService: SocketService
): ViewModel() {
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _socketPredictionResult = MutableStateFlow(SocketResponse())
    val socketPredictionResult = _socketPredictionResult.asStateFlow()

    private val _multiplePredictions = MutableStateFlow<Response<ResponseMultiplePredictions>>(Response.NullResponse())
    val multiplePredictions = _multiplePredictions.asStateFlow()

    private val _singlePredictions = MutableStateFlow<Response<ResponsePrediction>>(Response.NullResponse())
    val singlePrediction = _singlePredictions.asStateFlow()

    private val _detectedObjects = MutableStateFlow<MutableList<DetectedObjectItem>>(mutableListOf())
    val detectedObjects = _detectedObjects.asStateFlow()

    var objects: MutableList<Objects> = mutableListOf()

    val targetResolution = dataStore.getString(PreferencesKey.targetResolution)

    // Submit detected object
    fun submitDetectedObjectItem(data: List<DetectedObjectItem>) {
        _detectedObjects.value.clear()
        _detectedObjects.value.addAll(data)
    }

    fun submitImages(data: List<Objects>) {
        objects.clear()
        objects.addAll(data)

        val listRequestImage: MutableList<RequestImageIndexed> = mutableListOf()
        data.map {
            it.bitmap.toBase64()?.let { str ->
                listRequestImage.add(
                    RequestImageIndexed(
                        it.index,
                        str
                ))
            }
        }

        val request = RequestMultipleImage(listRequestImage)

        sendMultipleImages(request)
    }

    private fun sendMultipleImages(request: RequestMultipleImage) {
        viewModelScope.launch {
            repository.uploadMultipleImage(request).collectLatest { res ->
                _multiplePredictions.value = res

                // Check if result is success and not empty
                if (res is Response.Success) {
                    if (res.data.result.isNotEmpty()) {
                        val check = res.data.result.filter { it.className != "Unknown" }
                        if (check.isEmpty()) {
                            _uiEvent.emit(UiEvent.DetectionFailed)
                        }else {
                            _uiEvent.emit(UiEvent.ShowDetails)
                        }
                    }
                }else if (res is Response.Error) {
                    _uiEvent.emit(UiEvent.NotConnected)
                }
            }
        }
    }

    fun detectionFailed() {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.DetectionFailed)
        }
    }

    // Live Detection
    fun connectToSocket() {
        viewModelScope.launch {
            when(val connection = socketService.initSocket()) {
                is Response.Success -> {
                    _uiEvent.emit(UiEvent.SocketConnected)

                    socketService.observeSocket()
                        .collectLatest {
                            _socketPredictionResult.value = _socketPredictionResult.value.copy(
                                className = it.className,
                                confidence = it.confidence.toString(),
                                error = it.error
                            )
                        }
                }
                is Response.ApiError -> {
                    _uiEvent.emit(UiEvent.ConnectionClosed)
                }
                else -> {
                    if (connection is Response.Error) {
                        _uiEvent.emit(UiEvent.Error(
                            connection.exception.message
                                ?: connection.exception.toString()
                        ))
                    }
                }
            }
        }
    }

    fun sendSingleImage(imageBase64: String) {
        viewModelScope.launch {
            repository.uploadSingleImage(RequestImage(imageBase64))
                .collectLatest { res ->
                    _singlePredictions.value = res

                    if (res is Response.Success && res.data.className != "Unknown") {
                        _uiEvent.emit(UiEvent.ShowDetails)
                    }

                    if (res is Response.Success && res.data.className == "Unknown") {
                        _uiEvent.emit(UiEvent.DetectionFailed)
                    }

                    if (res is Response.Error) {
                        _uiEvent.emit(UiEvent.NotConnected)
                    }
                }
        }
    }

    fun sendImage(imageBase64: String) {
        viewModelScope.launch {
            socketService.sendImage(RequestImage(imageBase64))

        }
    }

    fun closeSocket() {
        viewModelScope.launch {
            socketService.closeSocket()
        }
    }

    data class Objects(
        val index: Int,
        val detectedObject: DetectedObject,
        val bitmap: Bitmap,
        var prediction: String = "",
        var confidence: String = "",
    )
}