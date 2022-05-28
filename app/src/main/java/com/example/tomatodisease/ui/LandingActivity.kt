package com.example.tomatodisease.ui

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tomatodisease.analyzer.DetectorOptions
import com.example.tomatodisease.databinding.ActivityLandingBinding
import com.example.tomatodisease.databinding.FragmentImageDetectionBinding
import com.example.tomatodisease.utils.getBitmapFromUri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SplashWaitTime: Long = 2000

class LandingActivity : AppCompatActivity() {

    private val binding by lazy { ActivityLandingBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        MainScope().launch {
            delay(SplashWaitTime)

            Intent(this@LandingActivity, MainActivity::class.java).also {
                startActivity(it)
                finish()
            }
        }
    }
}