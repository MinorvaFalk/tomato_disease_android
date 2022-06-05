package com.example.tomatodisease.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tomatodisease.databinding.ActivityLandingBinding
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