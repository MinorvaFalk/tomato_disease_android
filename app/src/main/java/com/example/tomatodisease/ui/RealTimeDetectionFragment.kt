package com.example.tomatodisease.ui

import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRect
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.tomatodisease.analyzer.ObjectDetectorAnalyzer
import com.example.tomatodisease.databinding.FragmentRealTimeDetectionBinding
import com.example.tomatodisease.domain.model.DetectedObjectItem
import com.example.tomatodisease.ui.viewmodels.MainViewModel
import com.example.tomatodisease.utils.*
import com.google.mlkit.vision.camera.CameraXSource
import com.google.mlkit.vision.objects.DetectedObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max

@AndroidEntryPoint
class RealTimeDetectionFragment : Fragment() {
    private var _binding: FragmentRealTimeDetectionBinding? = null
    private val binding get() = _binding!!

    private var detected: List<DetectedObject>? = null
    private var imgProxy: ImageProxy? = null
    private var croppedImage: Bitmap? = null

    private var cameraXSource: CameraXSource? = null

    private val viewModel by activityViewModels<MainViewModel>()

    private val cameraExecutor by lazy { Executors.newSingleThreadExecutor() }

    private val timer = (0..Int.MAX_VALUE)
        .asSequence()
        .asFlow()
        .onEach { delay(1000) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRealTimeDetectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (requireContext().allPermissionsGranted()) {
            viewModel.connectToSocket()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.apply {
            cameraOverlay.clearView()
            includeDetected.root.isVisible = false
            tvInfo.isVisible = true
            btnSettings.setOnClickListener {
                findNavController().navigate(RealTimeDetectionFragmentDirections.openSettings())
            }

            includeDetected.btnShowMore.setOnClickListener {
                findNavController().navigate(RealTimeDetectionFragmentDirections.toDetectedObjects())
            }

            root.systemUiVisibility = SYSTEM_UI_FLAG_FULLSCREEN
        }

        subscribeObserver()
    }

    // Collect state changes from viewModel
    private fun subscribeObserver() {
        collectLatestLifecycleFlow(viewModel.socketPredictionResult) { response ->
            if (response.className.isNotBlank() && response.error.isNullOrBlank()) {
                // Draw Bounding box
                detected?.let { obj ->
                    imgProxy?.let { img ->

                        binding.apply {
                            includeDetected.apply {
                                root.isVisible = true
                                tvInfo.isVisible = false
                                tvDetectedClass.text = response.className
                                tvConfidenceLevel.text = response.confidence
                                imgPreview.setImageBitmap(croppedImage)
                            }

                            if (response.className != "Unknown") {
                                includeDetected.btnShowMore.isVisible = true
                                viewModel.submitDetectedObjectItem(
                                    listOf(
                                        DetectedObjectItem(
                                            0,
                                            response.className,
                                            response.confidence,
                                            croppedImage
                                        )
                                    )
                                )
                            }else {
                                includeDetected.btnShowMore.isVisible = false
                            }

                            cameraOverlay.apply {
                                post {
                                    drawDetectionResults(obj, img)
                                }
                            }
                        }
                    }
                }
            }else {
                if (response.error != null) {
                    requireContext().showToast(response.error)
                }

                binding.apply {
                    includeDetected.root.isVisible = false
                    tvInfo.isVisible = true
                }
            }
        }

        collectLatestLifecycleFlow(viewModel.uiEvent) { event ->
            when(event) {
                is UiEvent.Error -> {
                    requireContext().showDialogNoInternet()
                        .setPositiveButton("Retry") { dialog, which ->
                            viewModel.connectToSocket()
                        }
                        .setNeutralButton("Back") { dialog, which ->
                            findNavController().navigateUp()
                        }
                        .show()
                }
                is UiEvent.ConnectionClosed -> {
                    requireContext().showSnackbar("Connection closed", binding.root)
                }
                is UiEvent.SocketConnected -> {
                    startCamera()
                }

                else -> {

                }
            }
        }
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    buildPreviewView(),
                    buildImageAnalysis()
                )
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))

    }

    // Preview view
    private fun buildPreviewView(): Preview {
        val preview = Preview.Builder()
            .apply {

            }
            .build()
            .also {
                binding.apply {

                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
            }

        return preview
    }

    // Image analysis using object detection
    private fun buildImageAnalysis(): ImageAnalysis {
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, ObjectDetectorAnalyzer { task, imageProxy ->
                    // Handle on object detected
                    task.addOnSuccessListener { detectedObjects ->
                        debugPrint(detectedObjects)
                        if (detectedObjects.isNullOrEmpty()) {
                            binding.apply {
                                includeDetected.root.isVisible = false
                                cameraOverlay.clearView()
                                tvInfo.isVisible = true
                            }

                            return@addOnSuccessListener
                        }

                        detected = detectedObjects
                        imgProxy = imageProxy
                        for (detectedObject in detectedObjects) {
                            croppedImage = getCroppedImage(detectedObject, imageProxy)

                            croppedImage?.toBase64()?.let { str ->
                                viewModel.sendImage(str)
                            }
                        }
                        imageProxy.close()
                    }

                    // Handle on failure
                    task.addOnFailureListener { e ->
                        Log.e(TAG, e.message ?: e.toString())

                        // Close image proxy if error occurred
                        imageProxy.close()
                    }

                    task.addOnCompleteListener {
                        imageProxy.close()
                    }
                })
            }
        return imageAnalyzer
    }

    private fun cropImage(detectedObject: DetectedObject): Bitmap? {
        return binding.viewFinder.bitmap?.cropBitmap(detectedObject.boundingBox)
    }

    private fun getCroppedImage(detectedObject: DetectedObject, imageProxy: ImageProxy): Bitmap? {
        val rotation = imageProxy.imageInfo.rotationDegrees

        val reverseDimens = rotation == 90 || rotation == 270
        val imageWidth = if (reverseDimens) imageProxy.height else imageProxy.width
        val imageHeight = if (reverseDimens) imageProxy.width else imageProxy.height
        val scaleFactor =
            max(
                imageWidth / binding.viewFinder.width.toFloat(),
                imageHeight/ binding.viewFinder.height.toFloat()
            )

        val diffWidth = abs(binding.viewFinder.width - imageWidth / scaleFactor) / 2
        val diffHeight = abs(binding.viewFinder.height - imageHeight / scaleFactor) / 2

        val actualRectBoundingBox = RectF(
            (detectedObject.boundingBox.left / scaleFactor) + diffWidth,
            (detectedObject.boundingBox.top / scaleFactor) + diffHeight,
            (detectedObject.boundingBox.right / scaleFactor) + diffWidth,
            (detectedObject.boundingBox.bottom / scaleFactor) + diffHeight
        ).toRect()

        Log.i(TAG, "Bounding Box: $actualRectBoundingBox")

        imageProxy.close()

        return binding.viewFinder.bitmap?.let {
            Bitmap.createBitmap(
                it,
                actualRectBoundingBox.left,
                actualRectBoundingBox.top,
                actualRectBoundingBox.width(),
                actualRectBoundingBox.height()
            )
        }
    }

    private fun debugPrint(detectedObjects: List<DetectedObject>) {
        detectedObjects.forEachIndexed { index, detectedObject ->
            val box = detectedObject.boundingBox

            Log.d(TAG, "Detected object: $index")
            Log.d(TAG, " trackingId: ${detectedObject.trackingId}")
            Log.d(TAG, " boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})")
            detectedObject.labels.forEach {
                Log.d(TAG, " categories: ${it.text}")
                Log.d(TAG, " confidence: ${it.confidence}")
            }
        }
    }

    companion object {
        private const val TAG = "LiveDetectionFragment"
    }

    private fun <T> Fragment.collectLatestLifecycleFlow(
        flow: Flow<T>,
        collect: suspend (T) -> Unit,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collectLatest(collect)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.closeSocket()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        binding.cameraOverlay.clearView()
    }

}