package com.example.tomatodisease.domain.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFERENCES_NAME = "user_preferences"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)

private const val TAG = "DataStoreRepository"

@Singleton
class DataStoreRepository @Inject constructor(
    private val context: Context,
) {
    suspend fun saveString(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { pref ->
            pref[key] = value
        }
    }

    suspend fun saveBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { pref ->
            pref[key] = value
        }
    }

    suspend fun saveInt(key: Preferences.Key<Int>, value: Int) {
        context.dataStore.edit { pref ->
            pref[key] = value
        }
    }

    fun getString(key: Preferences.Key<String>): Flow<String> =
        context.dataStore.data.catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { pref ->
            pref[key] ?: ""
        }

    fun getBoolean(key: Preferences.Key<Boolean>): Flow<Boolean> =
        context.dataStore.data.map { pref ->
            pref[key] ?: false
        }

    fun getInt(key: Preferences.Key<Int>): Flow<Int> =
        context.dataStore.data.map { pref ->
            pref[key] ?: 0
        }


}
