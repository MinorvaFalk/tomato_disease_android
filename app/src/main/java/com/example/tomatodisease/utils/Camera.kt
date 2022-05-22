package com.example.tomatodisease.utils

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.camera.core.CameraSelector
import com.example.tomatodisease.ui.SettingsFragment

private const val TAG = "CameraUtils"

fun getCameraCharacteristics(
    context: Context,
): CameraCharacteristics? {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    try {
        val cameraList = listOf(*cameraManager.cameraIdList)
        for (availableCameraId in cameraList) {
            val availableCameraCharacteristics = cameraManager.getCameraCharacteristics(
                availableCameraId!!
            )
            val availableLensFacing =
                availableCameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                    ?: continue
            if (availableLensFacing == CameraSelector.LENS_FACING_BACK) {
                return availableCameraCharacteristics
            }
        }
    } catch (e: CameraAccessException) {
        Log.e(TAG, e.message ?: e.toString())
    }
    return null
}
