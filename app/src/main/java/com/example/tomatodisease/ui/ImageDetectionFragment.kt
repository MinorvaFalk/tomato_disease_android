@file:Suppress("DEPRECATION")

package com.example.tomatodisease.ui

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import com.example.tomatodisease.analyzer.DetectorOptions
import com.example.tomatodisease.databinding.FragmentImageDetectionBinding
import com.example.tomatodisease.domain.model.DetectedObjectItem
import com.example.tomatodisease.utils.*
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ImageDetectionFragment : Fragment() {
    private var _binding: FragmentImageDetectionBinding? = null
    private val binding
        get() = _binding!!

    private var imageUri: Uri? = null

    private val viewModel by activityViewModels<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImageDetectionBinding.inflate(inflater, container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!allPermissionsGranted(requireContext())) {
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        binding.apply {
            includeBottom.apply {
                btnCamera.setOnClickListener { startCameraIntentForResult() }
                btnGallery.setOnClickListener { startChooseImageIntentForResult() }

                btnDetailedInfo.apply {
                    setOnClickListener {
                        findNavController().navigate(ImageDetectionFragmentDirections.toDetectedObjects())
                    }
                }
            }
        }

        subscribeObserver()
    }

    // Observer for UI Update
    private fun subscribeObserver() {
        collectLatestLifecycleFlow(viewModel.detectionState) {
            binding.apply {
                progressBar.isVisible = it.isLoading
            }
        }

        collectLatestLifecycleFlow(viewModel.uiEvent) { event ->
            binding.apply {
                when (event) {
                    // On error
                    is UiEvent.Error -> {
                        includeBottom.btnDetailedInfo.isVisible = false
                        Snackbar.make(root, event.msg, Snackbar.LENGTH_SHORT).show()
                    }

                    // On object detected
                    is UiEvent.ShowDetails -> {
                        includeBottom.btnDetailedInfo.isVisible = true
                    }

                    // On detection failed
                    is UiEvent.DetectionFailed -> {
                        includeBottom.btnDetailedInfo.isVisible = false
                        Snackbar.make(root, "Not detecting any object", Snackbar.LENGTH_SHORT)
                            .setAnchorView(includeBottom.root)
                            .show()
                    }
                }
            }
        }
    }

    // Detect image using object detector
    private fun detectImage() {
        // Loading State
        viewModel.updateState(MainViewModel.DetectionState(isLoading = true))

        try {
            val imageBitmap = requireActivity().getBitmapFromUri(imageUri!!)
            val image = InputImage.fromBitmap(imageBitmap!!, 0)

            val objectDetectedObject = ObjectDetection.getClient(DetectorOptions.SINGLE_IMAGE_MULTIPLE_OBJECT)
            objectDetectedObject.process(image)
                .addOnSuccessListener { detectedObjects ->
                    // Object found state
                    viewModel.updateState(MainViewModel.DetectionState(detectedObjects = detectedObjects))

                    debugPrint(detectedObjects)

                    binding.previewCanvas.apply {
                        setImageBitmap(imageBitmap)
                        post {
                            drawDetectionResults(detectedObjects)
                            getDetectedObjectData(detectedObjects)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Error found state
                    viewModel.updateState(MainViewModel.DetectionState(error = e))

                    Log.e(TAG, e.message ?: e.toString())
                }

        } catch (e: Exception) {
            Log.e(TAG, e.message ?: e.toString())
            imageUri = null
        }
    }

    // Pass detected object info into ViewModel
    private fun getDetectedObjectData(objects: List<DetectedObject>) {
        val detectedObjectItem = mutableListOf<DetectedObjectItem>()

        objects.map {
            var className: String? = null
            var confidence: Float? = null


            for (label in it.labels) {
                className = label.text
                confidence = label.confidence
            }

            detectedObjectItem.add(DetectedObjectItem(
                it.trackingId,
                className,
                confidence,
                binding.previewCanvas.getDetectedImage(it.boundingBox)
            ))
        }

        viewModel.submitDetectedObjectItem(detectedObjectItem)
    }

    private fun startCameraIntentForResult() {
        // Clear image uri and preview
        imageUri = null

        // Start image capture
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, "New Picture")
                values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
                imageUri = requireActivity().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

                startActivityForResult(
                    takePictureIntent,
                    REQUEST_IMAGE_CAPTURE
                )
            }
        }
    }

    private fun startChooseImageIntentForResult() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Select Picture"),
            REQUEST_CHOOSE_IMAGE
        )
    }

    // Debug print detected object
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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            detectImage()
        }else if (requestCode == REQUEST_CHOOSE_IMAGE && resultCode == Activity.RESULT_OK) {
            imageUri = data!!.data
            detectImage()
        } else {
            super.onActivityResult(requestCode, resultCode, data)

            Toast.makeText(requireContext(), imageUri.toString(), Toast.LENGTH_SHORT).show()

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

    companion object {
        private const val TAG = "ImageDetectionFragment"
        private const val REQUEST_IMAGE_CAPTURE = 1001
        private const val REQUEST_CHOOSE_IMAGE = 1002
    }

}