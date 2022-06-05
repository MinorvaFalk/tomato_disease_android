package com.example.tomatodisease.domain.model

import android.graphics.Rect
import java.util.*

data class StillImageDetected(
    val id: Int,
    val image: String,
    val actualBoundingBox: Rect,
)