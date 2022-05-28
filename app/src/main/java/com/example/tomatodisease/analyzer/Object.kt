package com.example.tomatodisease.analyzer

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

typealias DetectorListener = (obj: Task<MutableList<DetectedObject>>, img: ImageProxy) -> Unit

object DetectorOptions {
    val SINGLE_IMAGE_MULTIPLE_OBJECT = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
        .enableClassification()
        .enableMultipleObjects()
        .build()

    val STREAM_MULTIPLE_OBJECT = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableClassification()
        .enableMultipleObjects()
        .build()

    val STREAM_SINGLE_OBJECT = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableClassification()
        .build()
}

@SuppressLint("UnsafeOptInUsageError")
class ObjectDetectorAnalyzer(
    private val listener: DetectorListener
) : ImageAnalysis.Analyzer {
    // Object Detector configuration
    private val customModel = LocalModel.Builder()
        .setAssetFilePath("lite-model_aiy_vision_classifier_plants_V1_3.tflite")
        .build()

    private val customObjectDetectorOptions = CustomObjectDetectorOptions.Builder(customModel)
        .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
        .enableClassification()
        .setClassificationConfidenceThreshold(0.5f)
        .setMaxPerObjectLabelCount(3)
        .build()

    private val customObjectDetector = ObjectDetection.getClient(customObjectDetectorOptions)

    private val objectDetector = ObjectDetection.getClient(DetectorOptions.STREAM_SINGLE_OBJECT)

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
