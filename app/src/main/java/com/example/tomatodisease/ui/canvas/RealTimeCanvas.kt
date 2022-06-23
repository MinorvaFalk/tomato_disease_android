package com.example.tomatodisease.ui.canvas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.camera.core.ImageProxy
import com.example.tomatodisease.utils.CanvasPreset.COLORS
import com.example.tomatodisease.utils.CanvasPreset.NUM_COLORS
import com.example.tomatodisease.utils.CanvasPreset.STROKE_WIDTH
import com.example.tomatodisease.utils.getActualBoundingBox
import com.google.mlkit.vision.objects.DetectedObject
import kotlin.math.abs
import kotlin.math.max

class RealTimeCanvas(
    context: Context,
    attrs: AttributeSet?
) : View(context, attrs) {

    companion object {
        private const val TAG = "DetectedObjectsView"

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

    fun clearView() {
        transformedResults = emptyList()

        postInvalidate()
    }


    fun drawDetectionResults(results: List<DetectedObject>, imgProxy: ImageProxy) {

        val rotation = imgProxy.imageInfo.rotationDegrees

        val reverseDimens = rotation == 90 || rotation == 270
        val imageWidth = if (reverseDimens) imgProxy.height else imgProxy.width
        val imageHeight = if (reverseDimens) imgProxy.width else imgProxy.height

        // Get scale size based width/height
        val scaleFactor =
            max(imageWidth / width.toFloat(), imageHeight / height.toFloat())

        // Calculate the total padding (based center inside scale type)
        val diffWidth = abs(width - imageWidth / scaleFactor) / 2
        val diffHeight = abs(height - imageHeight / scaleFactor) / 2

        transformedResults = results.map { result ->
            val colorID =
                if (result.trackingId == null) 0
                else abs(result.trackingId!! % NUM_COLORS)

            val actualRectBoundingBox = getActualBoundingBox(result.boundingBox, scaleFactor, diffWidth, diffHeight)
            TransformedDetectionResult(actualRectBoundingBox, result.boundingBox, boxPaints[colorID])
        }


        Log.d(
            TAG,
            "srcImage: ${imgProxy.width}/${imgProxy.height} - imageView: ${width}/${height} => scaleFactor: $scaleFactor"
        )


        // Invalid to re-draw the canvas
        // Method onDraw will be called with new data.
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw rectangle for each bounding box
        transformedResults.map { result ->
            canvas.drawRect(result.actualBoxRectF, result.paint)
        }
    }
}