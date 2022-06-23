package com.example.tomatodisease.utils

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val TAG = "Utils.Helpers"

fun Context.showToast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Context.showSnackbar(msg: String, view: View) {
    Snackbar.make(this, view, msg, Snackbar.LENGTH_SHORT).show()
}

fun Context.showDialogNoInternet(): MaterialAlertDialogBuilder {
    return MaterialAlertDialogBuilder(this)
        .setTitle("Failed to connect to server")
        .setMessage("Please try again later")
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

fun bitmapToFile(bitmap: Bitmap, fileNameToSave: String): File? { // File name like "image.png"
    //create a file to write bitmap data
    var file: File? = null
    return try {
        file = File(Environment.getExternalStorageDirectory().toString() + File.separator + fileNameToSave)
        file.createNewFile()

        //Convert bitmap to byte array
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos) // YOU can also save it in JPEG
        val bitmapdata = bos.toByteArray()

        //write the bytes in file
        val fos = FileOutputStream(file)
        fos.write(bitmapdata)
        fos.flush()
        fos.close()
        file
    } catch (e: Exception) {
        e.printStackTrace()
        file // it will return null
    }
}