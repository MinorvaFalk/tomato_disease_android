package com.example.tomatodisease.utils

sealed class UiEvent {
    data class Error(val msg: String) : UiEvent()
    object ShowDetails : UiEvent()
    object DetectionFailed : UiEvent()
}