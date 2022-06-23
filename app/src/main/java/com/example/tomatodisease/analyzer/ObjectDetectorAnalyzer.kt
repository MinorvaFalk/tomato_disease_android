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

private val objectDetector = ObjectDetection.getClient(DetectorOptions.STREAM_SINGLE_OBJECT)

object DetectorOptions {
    val SINGLE_IMAGE_MULTIPLE_OBJECT = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
        .enableMultipleObjects()
        .build()

    val STREAM_SINGLE_OBJECT = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .build()
}

typealias DetectorListener = (obj: Task<MutableList<DetectedObject>>, img: ImageProxy) -> Unit

// Analyzer that used for CameraX PreviewView image analysis
@SuppressLint("UnsafeOptInUsageError")
class ObjectDetectorAnalyzer(
    private val listener: DetectorListener
) : ImageAnalysis.Analyzer {

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            listener(CustomDetector.realTimeDetector.process(image), imageProxy)
        }
    }

    companion object {
        private const val TAG = "ObjectDetectorAnalyzer"
    }
}

object CustomDetector {
    private val localModel = LocalModel.Builder()
        .setAssetFilePath("mobile_net.tflite")
        .build()

    val stillImageOptions = CustomObjectDetectorOptions.Builder(localModel)
        .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
        .enableMultipleObjects()
        .build()

    val realTimeOptions = CustomObjectDetectorOptions.Builder(localModel)
        .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
        .build()

    val realTimeDetector = ObjectDetection.getClient(realTimeOptions)
}

