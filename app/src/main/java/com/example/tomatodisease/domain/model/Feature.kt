package com.example.tomatodisease.domain.model

import androidx.navigation.NavDirections
import com.example.tomatodisease.ui.ChooserFragmentDirections

data class Feature(
    val id: Int,
    val name: String,
    val description: String? = null,
    val dest: NavDirections
)

val features = listOf(
    Feature(1,"Live Detection", "Detect plant disease realtime using camera", ChooserFragmentDirections.toLiveDetection()),
    Feature(2,"Image Detection", "Detect plant disease using image from library", ChooserFragmentDirections.toImageDetection())
)