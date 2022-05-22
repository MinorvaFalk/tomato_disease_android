package com.example.tomatodisease.ui

import android.graphics.Rect
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tomatodisease.analyzer.ObjectDetectorAnalyzer
import com.example.tomatodisease.databinding.FragmentLiveDetectionBinding
import com.example.tomatodisease.utils.REQUEST_CODE_PERMISSIONS
import com.example.tomatodisease.utils.REQUIRED_PERMISSIONS
import com.example.tomatodisease.utils.allPermissionsGranted
import com.google.mlkit.vision.objects.DetectedObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class LiveDetectionFragment : Fragment() {
    private var _binding: FragmentLiveDetectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private lateinit var cameraExecutor: ExecutorService

    private var previewHeight: Int = 0
    private var previewWidth: Int = 0

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

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.apply {
            btnSettings.setOnClickListener {
                findNavController().navigate(LiveDetectionFragmentDirections.openSettings())
            }

            root.systemUiVisibility = SYSTEM_UI_FLAG_FULLSCREEN
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().apply {
                setupResolution(this)
            }.build().also {
                binding.apply {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                    var width = viewFinder.width * viewFinder.scaleX
                    var height = viewFinder.height * viewFinder.scaleY
                    val rotation = viewFinder.display.rotation
                    if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                        val temp = width
                        width = height
                        height = temp
                    }

                    previewHeight = height.toInt()
                    previewWidth = width.toInt()
                }
            }


            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ObjectDetectorAnalyzer { task, img ->
                        task.addOnSuccessListener { detectedObj ->
                            debugPrint(detectedObj)
                            val rotation = img.imageInfo.rotationDegrees

                            val reverseDimens = rotation == 90 || rotation == 270
                            val width = if (reverseDimens) img.height else img.width
                            val height = if (reverseDimens) img.width else img.height

                            val bounds = detectedObj.map { obj ->
                                obj.boundingBox.transform(width, height)
                            }

                            draw(bounds)
                        }
                        task.addOnFailureListener { e ->
                            Log.e(TAG, e.message ?: e.toString())
                        }
                        task.addOnCompleteListener {
                            img.close()
                        }
                    })
                }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))

    }

    private fun draw(bounds: List<RectF>) {
        binding.cameraOverlay.post {
            binding.cameraOverlay.drawObjBounds(bounds)
        }
    }

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

    private fun Rect.transform(width: Int, height: Int): RectF {
        val scaleX = previewWidth / width.toFloat()
        val scaleY = previewHeight / height.toFloat()

        // Scale all coordinates to match preview
        val scaledLeft = scaleX * left
        val scaledTop = scaleY * top
        val scaledRight = scaleX * right
        val scaledBottom = scaleY * bottom
        return RectF(scaledLeft, scaledTop, scaledRight, scaledBottom)
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
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}