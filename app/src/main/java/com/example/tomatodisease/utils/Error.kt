package com.example.tomatodisease.utils

sealed class Error {
    object DetectedNothing : Error()
    data class ExceptionFound(val error: Exception): Error()
}