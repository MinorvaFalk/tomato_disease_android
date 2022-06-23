package com.example.tomatodisease.ui.canvas

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatImageView
import com.example.tomatodisease.utils.CanvasPreset.COLORS
import com.example.tomatodisease.utils.CanvasPreset.NUM_COLORS
import com.example.tomatodisease.utils.CanvasPreset.STROKE_WIDTH
import com.google.mlkit.vision.objects.DetectedObject
import kotlin.math.abs
import kotlin.math.max

class DetectedObjectsView(
    context: Context,
    attrs: AttributeSet?
) : AppCompatImageView(context, attrs) {

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

    fun drawBoundingBox(boundingBox: List<Rect>) {
        (drawable as? BitmapDrawable)?.bitmap?.let { srcImage ->
            val scaleFactor =
                max(srcImage.width / width.toFloat(), srcImage.height / height.toFloat())
            val diffWidth = abs(width - srcImage.width / scaleFactor) / 2
            val diffHeight = abs(height - srcImage.height / scaleFactor) / 2

            transformedResults = boundingBox.map { result ->
                val actualRectBoundingBox = RectF(
                    (result.left / scaleFactor) + diffWidth,
                    (result.top / scaleFactor) + diffHeight,
                    (result.right / scaleFactor) + diffWidth,
                    (result.bottom / scaleFactor) + diffHeight
                )
                TransformedDetectionResult(actualRectBoundingBox, result, boxPaints[0])
            }
            Log.d(
                TAG,
                "srcImage: ${srcImage.width}/${srcImage.height} - imageView: ${width}/${height} => scaleFactor: $scaleFactor"
            )

            invalidate()
        }
    }

    fun clear() {
        transformedResults = emptyList()

        postInvalidate()
    }

    fun drawDetectionResults(results: List<DetectedObject>) {
        (drawable as? BitmapDrawable)?.bitmap?.let { srcImage ->
            // Get scale size based width/height
            val scaleFactor =
                max(srcImage.width / width.toFloat(), srcImage.height / height.toFloat())
            // Calculate the total padding (based center inside scale type)
            val diffWidth = abs(width - srcImage.width / scaleFactor) / 2
            val diffHeight = abs(height - srcImage.height / scaleFactor) / 2

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
                "srcImage: ${srcImage.width}/${srcImage.height} - imageView: ${width}/${height} => scaleFactor: $scaleFactor"
            )
            // Invalid to re-draw the canvas
            // Method onDraw will be called with new data.
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw rectangle for each bounding box
        transformedResults.forEach { result ->
            canvas.drawRect(result.actualBoxRectF, result.paint)
        }
    }

    fun cropImage(boundingBox: Rect): Bitmap? {
        (drawable as? BitmapDrawable)?.bitmap?.let {
            return Bitmap.createBitmap(
                it,
                boundingBox.left,
                boundingBox.top,
                boundingBox.width(),
                boundingBox.height()
            )
        }

        return null
    }

    private fun cropBitMapBasedResult(result: TransformedDetectionResult): Bitmap? {
        (drawable as? BitmapDrawable)?.bitmap?.let {
            return Bitmap.createBitmap(
                it,
                result.originalBoxRectF.left,
                result.originalBoxRectF.top,
                result.originalBoxRectF.width(),
                result.originalBoxRectF.height()
            )
        }
        return null
    }
}