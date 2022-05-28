package com.example.tomatodisease.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.navigation.fragment.NavHostFragment
import com.example.tomatodisease.R
import com.example.tomatodisease.databinding.ActivityMainBinding
import com.example.tomatodisease.utils.REQUEST_CODE_PERMISSIONS
import com.example.tomatodisease.utils.REQUIRED_PERMISSIONS
import com.example.tomatodisease.utils.allPermissionsGranted
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
    }
}