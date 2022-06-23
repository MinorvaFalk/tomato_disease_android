package com.example.tomatodisease.ui

import android.annotation.SuppressLint
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
import androidx.navigation.fragment.findNavController
import com.example.tomatodisease.databinding.FragmentStillImageBinding
import com.example.tomatodisease.domain.model.DetectedObjectItem
import com.example.tomatodisease.domain.model.Response
import com.example.tomatodisease.ui.viewmodels.MainViewModel
import com.example.tomatodisease.utils.*
import com.example.tomatodisease.utils.CameraHelper.REQUEST_CHOOSE_IMAGE
import com.example.tomatodisease.utils.CameraHelper.REQUEST_IMAGE_CAPTURE
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.objects.DetectedObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StillImageFragment : Fragment() {
    companion object {
        private const val TAG = "ImageDetectionBasicFragment"
    }

    private var _binding: FragmentStillImageBinding? = null
    private val binding get() = _binding!!

    private var imageUri: Uri? = null
    private val viewModel by activityViewModels<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStillImageBinding.inflate(inflater, container, false)
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
                btnDetailedInfo.visibility = View.INVISIBLE
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
    @SuppressLint("LongLogTag")
    private fun subscribeObserver() {
        // Collect result from API call
        collectLatestLifecycleFlow(viewModel.singlePrediction) { res ->
            Log.i(TAG, res.toString())

            when (res) {
                is Response.Success -> {
                    showLoading(false)
                    Log.i(TAG, "Detected : ${res.data.className}")

                    if (res.data.className.isBlank()) {
                        requireContext().showToast("Not detecting anything")
                        return@collectLatestLifecycleFlow
                    }

                    binding.includeBottom.apply {
                        btnDetailedInfo.setOnClickListener {
                            findNavController().navigate(StillImageFragmentDirections
                                .toResult(
                                    DetectedObjectItem(
                                        1,
                                        res.data.className,
                                        res.data.confidence,
                                        imageBitmap = requireActivity().getBitmapFromUri(imageUri!!)
                                    ),
                                    showSaveImage = false
                                ))
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
                    val msg = res.exception.message ?: res.toString()
                    showLoading(false)
                    Log.e(TAG, msg)
                }

                is Response.NullResponse -> {
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

    @SuppressLint("LongLogTag")
    private fun detectImage() {
        try {
            val imageBitmap = requireActivity().getBitmapFromUri(imageUri!!)

            binding.previewCanvas.apply {
                setImageBitmap(imageBitmap)

            }

            imageBitmap?.toBase64()?.let { str ->
                viewModel.sendSingleImage(str)
            }

        } catch (e: Exception) {
            Log.e(TAG, e.message ?: e.toString())
            imageUri = null
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
    @SuppressLint("LongLogTag")
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }
}