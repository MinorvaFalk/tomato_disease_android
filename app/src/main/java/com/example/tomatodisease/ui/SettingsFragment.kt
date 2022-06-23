package com.example.tomatodisease.ui

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.example.tomatodisease.R
import com.example.tomatodisease.domain.repository.DataStoreRepository
import com.example.tomatodisease.ui.viewmodels.SettingsViewModel
import com.example.tomatodisease.utils.getCameraCharacteristics
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var dataStore: DataStoreRepository
    private lateinit var resolutionEntries: Array<String?>

    private val viewModel by viewModels<SettingsViewModel>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.live_detection_preference, rootKey)

        // Get camera resolution size
        getCameraCharacteristics(
            requireActivity(),
        )?.let {
            val map = it.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val outputSizes = map!!.getOutputSizes(SurfaceTexture::class.java)
            resolutionEntries = arrayOfNulls(outputSizes.size)
            for (i in outputSizes.indices) {
                resolutionEntries[i] = outputSizes[i].toString()
            }
        }

        val prefEntries = if (resolutionEntries.isNotEmpty()) resolutionEntries else cameraResolution

        findPreference<ListPreference?>(getString(R.string.camera_target_reso_pref))
            ?.apply {
                entries = prefEntries
                entryValues = prefEntries
                summary = if (entry == null) "Default" else entry
                setOnPreferenceChangeListener { _, newValue ->
                    summary = newValue.toString()

                    viewModel.saveResolution(newValue.toString())
                    return@setOnPreferenceChangeListener true
                }
            }

    }

    companion object {
        private const val TAG = "SettingsFragment"
        val cameraResolution = arrayOf(
            "2000x2000",
            "1600x1600",
            "1200x1200",
            "1000x1000",
            "800x800",
            "600x600",
            "400x400",
            "200x200",
            "100x100",
        )
    }
}