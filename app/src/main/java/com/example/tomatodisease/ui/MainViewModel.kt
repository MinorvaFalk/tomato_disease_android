package com.example.tomatodisease.ui

import android.util.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tomatodisease.domain.repository.DataStoreRepository
import com.example.tomatodisease.utils.PreferencesKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataStore: DataStoreRepository
): ViewModel() {

    val targetResolution = dataStore.getString(PreferencesKey.targetResolution)

}