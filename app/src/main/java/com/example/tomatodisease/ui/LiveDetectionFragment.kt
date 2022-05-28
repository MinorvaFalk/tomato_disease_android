package com.example.tomatodisease.ui

import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
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

@AndroidEntryPoint
class LiveDetectionFragment : Fragment() {
    private var _binding: FragmentLiveDetectionBinding? = null
    private val binding get() = _binding!!

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

    private fun subscribeObserver() {
        collectLatestLifecycleFlow(viewModel.detectionState) {

        }

        collectLatestLifecycleFlow(viewModel.detectedObjectItem) {
            if (it.isNotEmpty()) {
                binding.includeDetected.imgPreview.setImageBitmap(it[0].imageBitmap)
            }
        }

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

    private fun getDetectedObjectData(objects: List<DetectedObject>) {
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
                    binding.cameraOverlay.getDetectedImage(it.boundingBox, binding.viewFinder)
                )
            )
        }

        viewModel.submitDetectedObjectItem(detectedObjectItem)
    }

    // Preview view
    private fun buildPreviewView(): Preview {
        val preview = Preview.Builder().apply {
//            setupResolution(this)
        }.build().also {
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
                it.setAnalyzer(cameraExecutor, ObjectDetectorAnalyzer { task, img ->

                    task.addOnSuccessListener { detectedObjects ->
                        viewModel.updateState(MainViewModel.DetectionState(detectedObjects = detectedObjects))

                        debugPrint(detectedObjects)

                        binding.cameraOverlay.apply {
                            post {
                                drawDetectionResults(detectedObjects, img)
                                getDetectedObjectData(detectedObjects)
                            }
                        }
                    }
                    task.addOnFailureListener { e ->
                        Log.e(TAG, e.message ?: e.toString())
                    }
                    task.addOnCompleteListener {
                        img.close()
                    }
                })
            }
        return imageAnalyzer
    }

    // Setup camera preview resolution
    private fun setupResolution(preview: Preview.Builder){
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.targetResolution.collectLatest {
                val targetResolution = try {
                    Size.parseSize(it)
                } catch (e: Exception) {
                    null
                }

                if (targetResolution != null) {
                    preview.setTargetResolution(targetResolution)
                }
            }
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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}