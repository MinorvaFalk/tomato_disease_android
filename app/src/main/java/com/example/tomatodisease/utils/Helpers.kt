package com.example.tomatodisease.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.io.IOException

fun Context.showToast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Activity.convertUriToBitmap(imgUri: Uri): Bitmap? {
    return MediaStore.Images.Media.getBitmap(contentResolver, imgUri)
}

fun Activity.getBitmapFromUri(imageUri: Uri): Bitmap? {
    val bitmap = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, imageUri))
        } else {
            // Add Suppress annotation to skip warning by Android Studio.
            // This warning resolved by ImageDecoder function.
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        }
    } catch (ex: IOException) {
        null
    }

    // Make a copy of the bitmap in a desirable format
    return bitmap?.copy(Bitmap.Config.ARGB_8888, false)
}
