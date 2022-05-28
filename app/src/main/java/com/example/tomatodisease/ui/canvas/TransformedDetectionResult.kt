package com.example.tomatodisease.ui.canvas

import android.graphics.*

data class TransformedDetectionResult(
    val actualBoxRectF: RectF,
    val originalBoxRectF: Rect,
    val paint: Paint
)

data class LiveTransformedDetectionResult(
    val actualBoxRectF: RectF,
    val originalBoxRectF: Rect,
    val paint: Paint,
    val image: Bitmap,
)
