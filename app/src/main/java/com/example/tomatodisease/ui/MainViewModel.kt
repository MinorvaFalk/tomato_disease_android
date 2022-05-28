package com.example.tomatodisease.ui

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tomatodisease.domain.model.DetectedObjectItem
import com.example.tomatodisease.domain.repository.DataStoreRepository
import com.example.tomatodisease.utils.PreferencesKey
import com.example.tomatodisease.utils.UiEvent
import com.google.mlkit.vision.objects.DetectedObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    dataStore: DataStoreRepository
): ViewModel() {
    val targetResolution = dataStore.getString(PreferencesKey.targetResolution)

    // Handle detection
    private val _detectionState = MutableStateFlow(DetectionState())
    val detectionState = _detectionState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun updateState(state: DetectionState){
        viewModelScope.launch {
            if (state.error != null) {
                _uiEvent.emit(UiEvent.Error(state.error.message ?: state.error.toString()))
            }

            if (!state.detectedObjects.isNullOrEmpty()) {
                _uiEvent.emit(UiEvent.ShowDetails)
            }

            if (state.detectedObjects.isNullOrEmpty()) {
                _uiEvent.emit(UiEvent.DetectionFailed)
            }

            _detectionState.value = state
        }
    }

    fun detectObject(detectedObjects: List<DetectedObject>, imageProxy: ImageProxy, croppedBitmap: Bitmap) {
        viewModelScope.launch {
            _detectionState.value = DetectionState(isLoading = true)

            // Pass image to end point to check if it is tomato leaf

            _detectionState.value = DetectionState(isLoading = false, detectedObjects = detectedObjects, imageProxy = imageProxy, croppedBitmap = croppedBitmap)
        }
    }

    // List of detected objects to pass for another fragment
    private val _detectedObjectItem = MutableStateFlow(listOf<DetectedObjectItem>())
    val detectedObjectItem = _detectedObjectItem.asStateFlow()

    fun submitDetectedObjectItem(obj: List<DetectedObjectItem>) {
        _detectedObjectItem.value = obj
    }

    data class DetectionState(
        val isLoading: Boolean = false,
        val detectedObjects: List<DetectedObject>? = null,
        val imageProxy: ImageProxy? = null,
        val croppedBitmap: Bitmap? = null,
        val error: Exception? = null
    )
}