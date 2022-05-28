package com.example.tomatodisease.domain.model

sealed class Resource<T>(
    val data: T? = null,
    val error: Exception? = null
) {
    class Success<T>(data: T, err: Exception? = null): Resource<T>(data, err)
    class Error<T>(err: Exception, data: T? = null): Resource<T>(data, err)
}
