package com.example.tomatodisease.ui.canvas

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.media.Image
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.core.graphics.toRect
import androidx.core.view.drawToBitmap
import com.google.mlkit.vision.objects.DetectedObject
import kotlin.math.abs
import kotlin.math.max

class LiveDetectedObjectsView(
    context: Context,
    attrs: AttributeSet?
) : View(context, attrs) {

    companion object {
        private const val TAG = "DetectedObjectsView"
        private const val NUM_COLORS = 10
        private const val STROKE_WIDTH = 4.0f
        private val COLORS =
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

    private val numColors = COLORS.size
    private val boxPaints = Array(numColors) { Paint() }

    private var transformedResults = listOf<TransformedDetectionResult>()

    init {
        for (i in 0 until numColors) {
            boxPaints[i] = Paint()
            boxPaints[i].color = COLORS[i][1]
            boxPaints[i].style = Paint.Style.STROKE
            boxPaints[i].strokeWidth = STROKE_WIDTH
        }
    }

    fun getDetectedImage(rect: Rect, previewView: PreviewView): Bitmap? {
        var bitmap: Bitmap? = null

        Log.d(TAG, transformedResults.toString())

        transformedResults.map {
            if (it.originalBoxRectF == rect) {
                bitmap = cropBitMapBasedResult(it, previewView)
                return@map
            }
        }

        return bitmap
    }

    fun drawDetectionResults(results: List<DetectedObject>, img: ImageProxy) {
        val rotation = img.imageInfo.rotationDegrees

        val reverseDimens = rotation == 90 || rotation == 270
        val imageWidth = if (reverseDimens) img.height else img.width
        val imageHeight = if (reverseDimens) img.width else img.height

        // Get scale size based width/height
        val scaleFactor =
            max(imageWidth / width.toFloat(), imageHeight/ height.toFloat())

        // Calculate the total padding (based center inside scale type)
        val diffWidth = abs(width - imageWidth / scaleFactor) / 2
        val diffHeight = abs(height - imageHeight / scaleFactor) / 2

        // Transform the original Bounding Box to actual bounding box based the display size of ImageView.
        transformedResults = results.map { result ->
            // Color for bounding box
            val colorID =
                if (result.trackingId == null) 0
                else abs(result.trackingId!! % NUM_COLORS)
            // Calculate to create new coordinates of Rectangle Box match on ImageView.
            val actualRectBoundingBox = RectF(
                (result.boundingBox.left / scaleFactor) + diffWidth,
                (result.boundingBox.top / scaleFactor) + diffHeight,
                (result.boundingBox.right / scaleFactor) + diffWidth,
                (result.boundingBox.bottom / scaleFactor) + diffHeight
            )
            // Transform to new object to hold the data inside.
            // This object is necessary to avoid performance
            TransformedDetectionResult(actualRectBoundingBox, result.boundingBox, boxPaints[colorID])
        }
        Log.d(
            TAG,
            "srcImage: ${imageWidth}/${imageHeight} - imageView: ${width}/${height} => scaleFactor: $scaleFactor"
        )
        // Invalid to re-draw the canvas
        // Method onDraw will be called with new data.
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw rectangle for each bounding box
        transformedResults.forEach { result ->
            canvas.drawRect(result.actualBoxRectF, result.paint)
        }
    }

    private fun cropBitMapBasedResult(result: TransformedDetectionResult, previewView: PreviewView): Bitmap? {
        val converted = result.actualBoxRectF.toRect()

        return Bitmap.createBitmap(
            previewView.bitmap!!,
            converted.left,
            converted.top,
            converted.width(),
            converted.height()
        )
    }
}