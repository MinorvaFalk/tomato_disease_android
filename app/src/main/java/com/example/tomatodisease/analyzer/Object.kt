package com.example.tomatodisease.analyzer

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

typealias DetectorListener = (obj: Task<MutableList<DetectedObject>>, img: ImageProxy) -> Unit

@SuppressLint("UnsafeOptInUsageError")
class ObjectDetectorAnalyzer(
    private val listener: DetectorListener
) : ImageAnalysis.Analyzer {
    // Object Detector configuration
    private val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .build()
    private val objectDetector = ObjectDetection.getClient(options)

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            listener(objectDetector.process(image), imageProxy)
        }
    }

    companion object {
        private const val TAG = "ObjectDetectorAnalyzer"
    }
}
