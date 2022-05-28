package com.example.tomatodisease.ui

import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.util.Size
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
import com.example.tomatodisease.R
import com.example.tomatodisease.analyzer.ObjectDetectorAnalyzer
import com.example.tomatodisease.databinding.FragmentLiveDetectionBinding
import com.example.tomatodisease.domain.model.DetectedObjectItem
import com.example.tomatodisease.utils.REQUEST_CODE_PERMISSIONS
import com.example.tomatodisease.utils.REQUIRED_PERMISSIONS
import com.example.tomatodisease.utils.UiEvent
import com.example.tomatodisease.utils.allPermissionsGranted
import com.google.mlkit.vision.objects.DetectedObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max

@AndroidEntryPoint
class LiveDetectionFragment : Fragment() {
    private var _binding: FragmentLiveDetectionBinding? = null
    private val binding get() = _binding!!

    private var trackingId: Int? = -1

    private val viewModel by activityViewModels<MainViewModel>()

    private val cameraExecutor by lazy { Executors.newSingleThreadExecutor() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveDetectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (allPermissionsGranted(requireContext())) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }


        binding.apply {
            btnSettings.setOnClickListener {
                findNavController().navigate(LiveDetectionFragmentDirections.openSettings())
            }

            includeDetected.btnShowMore.setOnClickListener {
                findNavController().navigate(LiveDetectionFragmentDirections.toDetectedObjects())
            }

            root.systemUiVisibility = SYSTEM_UI_FLAG_FULLSCREEN
        }

        subscribeObserver()
    }

    // Collect state changes from viewModel
    private fun subscribeObserver() {
        collectLatestLifecycleFlow(viewModel.detectionState) {
            showLoading(it.isLoading)

            it.detectedObjects?.let { detectedObjects ->
                getDetectedObjectData(detectedObjects, it.croppedBitmap!!)

                binding.cameraOverlay.apply {
                    post {
                        it.imageProxy?.let { img ->
                            drawDetectionResults(detectedObjects, img)
                        }
                    }
                }
            }

            it.imageProxy?.close()
        }

        // Handle detected objects
        // TODO: Deprecate
//        collectLatestLifecycleFlow(viewModel.detectedObjectItem) {
//            if (it.isNotEmpty()) {
//
//                binding.includeDetected.apply {
//                    root.isVisible = true
//                    imgPreview.setImageBitmap(it[0].imageBitmap)
//                }
//            }
//        }

        // Handle UI Event
        collectLatestLifecycleFlow(viewModel.uiEvent) { event ->
            binding.apply {
                when (event) {
                    is UiEvent.Error -> {
                        tvInfo.isVisible = true
                        includeDetected.root.isVisible = false
                    }

                    is UiEvent.ShowDetails -> {
                        tvInfo.isVisible = false
                        includeDetected.root.isVisible = true
                    }

                    is UiEvent.DetectionFailed -> {
                        tvInfo.isVisible = true
                        includeDetected.root.isVisible = false
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean){
        binding.apply {
            tvInfo.isVisible = true
            includeDetected.root.isVisible = false

            if (!isLoading) {
                tvInfo.setText(R.string.no_object_detected)
            }else {
                tvInfo.setText(R.string.loading)
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
            .build()
            .also {
                binding.apply {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
            }

        return preview
    }

    // Image analysis using object detection
    //
    // Detection will require user to be steady with device camera.
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
                            binding.cameraOverlay.clearView()
                            showLoading(false)

                            return@addOnSuccessListener
                        }

                        for (detectedObject in detectedObjects) {
                            Log.i(TAG, "current tracking ID: $trackingId")
                            Log.i(TAG, "detected tracking ID: ${detectedObject.trackingId}")

                            trackingId = detectedObject.trackingId
                            val croppedBitmap = getCroppedImage(detectedObject, imageProxy)

                            Log.i(TAG, "Cropped bitmap: "+ croppedBitmap.toString())

                            croppedBitmap?.let {
                                viewModel.detectObject(detectedObjects, imageProxy, croppedBitmap)
                            }
                        }
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

    private fun getDetectedObjectData(objects: List<DetectedObject>, croppedImage: Bitmap) {
        val detectedObjectItem = mutableListOf<DetectedObjectItem>()

        objects.map {
            var className: String? = null
            var confidence: Float? = null


            for (label in it.labels) {
                className = label.text
                confidence = label.confidence
            }

            detectedObjectItem.add(
                DetectedObjectItem(
                    it.trackingId,
                    className,
                    confidence,
                    croppedImage
                )
            )
        }

        binding.apply {
            tvInfo.isVisible = false
            includeDetected.apply {
                root.isVisible = true
                imgPreview.setImageBitmap(croppedImage)
                detectedObjectItem.map {

                    it.confidence?.let { conf ->
                        tvConfidenceLevel.text = conf.toString()
                    }

                    tvDetectedClass.text = if (it.className.isNullOrEmpty()) "Not Found" else it.className
                }
            }
        }

        viewModel.submitDetectedObjectItem(detectedObjectItem)
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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}