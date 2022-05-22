package com.example.tomatodisease.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tomatodisease.domain.repository.DataStoreRepository
import com.example.tomatodisease.utils.PreferencesKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStoreRepository
): ViewModel(){

    fun saveResolution(resolution: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.saveString(PreferencesKey.targetResolution, resolution)
        }
    }
}