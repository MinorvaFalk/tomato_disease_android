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
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import com.example.tomatodisease.analyzer.CustomDetector
import com.example.tomatodisease.databinding.FragmentMultipleObjDetectionBinding
import com.example.tomatodisease.domain.model.DetectedObjectItem
import com.example.tomatodisease.domain.model.Response
import com.example.tomatodisease.ui.viewmodels.MainViewModel
import com.example.tomatodisease.utils.*
import com.example.tomatodisease.utils.CameraHelper.REQUEST_CHOOSE_IMAGE
import com.example.tomatodisease.utils.CameraHelper.REQUEST_IMAGE_CAPTURE
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MultipleObjDetection : Fragment() {
    private var _binding: FragmentMultipleObjDetectionBinding? = null
    private val binding
        get() = _binding!!

    private var imageUri: Uri? = null

    private val viewModel by activityViewModels<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMultipleObjDetectionBinding.inflate(inflater, container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!requireContext().allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        binding.apply {
            includeBottom.apply {
                btnCamera.setOnClickListener {
                    previewCanvas.drawDetectionResults(emptyList())
                    startCameraIntentForResult()
                }
                btnGallery.setOnClickListener {
                    previewCanvas.drawDetectionResults(emptyList())
                    startChooseImageIntentForResult()
                }

                btnDetailedInfo.apply {
                    setOnClickListener {
                        findNavController().navigate(MultipleObjDetectionDirections.toDetectedObjects())
                    }
                }
            }
        }

        subscribeObserver()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.apply {
            progressBar.isVisible = isLoading
        }
    }

    // Observer for UI Update
    private fun subscribeObserver() {
        // Collect result from API call
        collectLatestLifecycleFlow(viewModel.multiplePredictions) { res ->
            when (res) {
                is Response.Success -> {
                    Log.i(TAG, "Detected : ${res.data.result}")

                    if (res.data.result.isEmpty()) {
                        viewModel.detectionFailed()
                        return@collectLatestLifecycleFlow
                    }

                    val objects = viewModel.objects
                    val detectedObject = mutableListOf<DetectedObjectItem>()
                    val newObject = mutableListOf<DetectedObject>()

                    // Mapping detected object to another fragment
                    res.data.result.map { result ->
                        // Create list of detectedObject to draw boundingBox
                        if (result.className != "Unknown") {
                            val new = objects[result.index].detectedObject
                            newObject.add(new)

                            detectedObject.add(DetectedObjectItem(
                                result.index,
                                result.className,
                                result.confidence,
                                objects[result.index].bitmap
                            ))
                        }

                    }
                    viewModel.submitDetectedObjectItem(detectedObject)

                    // Draw bounding box
                    binding.previewCanvas.apply {
                        post {
                            drawDetectionResults(newObject)
                            showLoading(false)
                        }
                    }

                }

                is Response.Loading -> {
                    showLoading(true)
                }

                is Response.ApiError -> {
                    showLoading(false)
                    Log.e(TAG, res.message)
                }

                is Response.Error -> {
                    showLoading(false)

                }

                else -> {
                    showLoading(false)
                }
            }


        }

        // Collect UiEvent
        collectLatestLifecycleFlow(viewModel.uiEvent) { event ->
            binding.apply {
                when (event) {
                    is UiEvent.NotConnected -> {
                        requireContext().showDialogNoInternet()
                            .setPositiveButton("Retry") { dialog, which ->
                                detectImage()
                            }
                            .setNeutralButton("Cancel") { dialog, which ->
                                dialog.dismiss()
                            }
                            .show()
                    }

                    // On error
                    is UiEvent.Error -> {
                        includeBottom.btnDetailedInfo.visibility = View.INVISIBLE
                        Snackbar.make(root, event.msg, Snackbar.LENGTH_SHORT).show()
                    }

                    // On object detected
                    is UiEvent.ShowDetails -> {
                        includeBottom.btnDetailedInfo.isVisible = true
                    }

                    // On detection failed
                    is UiEvent.DetectionFailed -> {
                        includeBottom.btnDetailedInfo.visibility = View.INVISIBLE
                        Snackbar.make(root, "Not detecting any object", Snackbar.LENGTH_SHORT)
                            .setAnchorView(includeBottom.root)
                            .show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun detectImage(){
        try {
            val imageBitmap = requireActivity().getBitmapFromUri(imageUri!!)
            val image = InputImage.fromBitmap(imageBitmap!!, 0)

            binding.previewCanvas.apply {
                setImageBitmap(imageBitmap)
            }

            val objectDetectedObject = ObjectDetection.getClient(CustomDetector.stillImageOptions)
            objectDetectedObject.process(image)
                .addOnSuccessListener { detectedObjects ->
                    binding.previewCanvas.clear()
                    // Return if no object detected
                    if (detectedObjects.isEmpty()) {
                        viewModel.detectionFailed()
                        return@addOnSuccessListener
                    }

                    val indexedImage = mutableListOf<MainViewModel.Objects>()
                    detectedObjects.mapIndexed { index, detectedObject ->
                        // Get cropped image then send to API
                        binding.previewCanvas.cropImage(detectedObject.boundingBox)?.let { bitmap ->
                            indexedImage.add(
                                MainViewModel.Objects(
                                index,
                                detectedObject,
                                bitmap
                            ))
                        }
                    }

                    viewModel.submitImages(indexedImage)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, e.message ?: e.toString())

                }
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: e.toString())
            imageUri = null
        }
    }

    private fun loadImageToCanvas() {
        binding.apply {
            val imageBitmap = requireActivity().getBitmapFromUri(imageUri!!)
            previewCanvas.setImageBitmap(imageBitmap)
        }
    }

    // Capture image from camera
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

    // Choose image from storage intent
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
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(
            "IMAGE_URI",
            imageUri
        )
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
    }

}