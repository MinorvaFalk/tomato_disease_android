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
    Feature(1,"Real-Time Detection", "Detect plant disease realtime", ChooserFragmentDirections.toLiveDetection()),
    Feature(2,"Multiple Object", "Detect multiple plant disease", ChooserFragmentDirections.toImageDetection()),
    Feature(3, "Still Image", "Detect plant disease using image from library", ChooserFragmentDirections.toImageDetectionBasic())
)