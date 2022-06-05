package com.example.tomatodisease.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.util.Base64
import java.io.ByteArrayOutputStream


fun getActualBoundingBox(
    boundingBox: Rect,
    scaleFactor: Float,
    diffWidth: Float,
    diffHeight: Float
) = RectF(
    (boundingBox.left / scaleFactor) + diffWidth,
    (boundingBox.top / scaleFactor) + diffHeight,
    (boundingBox.right / scaleFactor) + diffWidth,
    (boundingBox.bottom / scaleFactor) + diffHeight
)

fun Bitmap.cropBitmap(boundingBox: Rect): Bitmap = Bitmap.createBitmap(
    this,
    boundingBox.left,
    boundingBox.top,
    boundingBox.width(),
    boundingBox.height()
)

fun Bitmap.toBase64() : String? {
    val outputStream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, 65, outputStream)

    return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)

//    val out = ByteArrayOutputStream()
//    this.compress(Bitmap.CompressFormat.JPEG, 100, out)
//    val byte = out.toByteArray()
//
//    return Base64.encodeToString(byte, Base64.DEFAULT)
}

object CanvasPreset {
    const val NUM_COLORS = 10
    const val STROKE_WIDTH = 4.0f
    val COLORS =
        arrayOf(
            intArrayOf(Color.BLACK, Color.WHITE),
            intArrayOf(Color.WHITE, Color.MAGENTA),
            intArrayOf(Color.BLACK, Color.LTGRAY),
            intArrayOf(Color.WHITE, Color.RED),
            intArrayOf(Color.WHITE, Color.BLUE),
            intArrayOf(Color.WHITE, Color.DKGRAY),
            intArrayOf(Color.BLACK, Color.CYAN),
            intArrayOf(Color.BLACK, Color.YELLOW),
            intArrayOf(Color.WHITE, Color.BLACK),
            intArrayOf(Color.BLACK, Color.GREEN)
        )
}

