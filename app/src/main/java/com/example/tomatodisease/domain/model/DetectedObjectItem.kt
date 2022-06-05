package com.example.tomatodisease.domain.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DetectedObjectItem(
    val id: Int,
    val className: String,
    val confidence: String,
    val imageBitmap: Bitmap?
): Parcelable