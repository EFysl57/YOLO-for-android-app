package com.example.yoladetection

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var detections: List<Detection> = emptyList()


    private var imageWidth = 640
    private var imageHeight = 640




    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 42f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    private val bgPaint = Paint().apply {
        color = Color.argb(180, 0, 0, 0)
        style = Paint.Style.FILL
    }

    fun updateDetections(
        detections: List<Detection>,
        imageWidth: Int,
        imageHeight: Int
    ) {
        this.detections = detections
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (detections.isEmpty()) return

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        for (det in detections) {

            if (det.box.size < 4) continue

            val left = det.box[0] * scaleX
            val top = det.box[1] * scaleY
            val right = det.box[2] * scaleX
            val bottom = det.box[3] * scaleY

            boxPaint.color = getColor(det.classId)

            canvas.drawRoundRect(
                left,
                top,
                right,
                bottom,
                16f,
                16f,
                boxPaint
            )

            val confidence = (det.confidence * 100).toInt()

            val label =
                "${det.className.replaceFirstChar { it.uppercase() }}  $confidence%"

            val textWidth = textPaint.measureText(label)

            var textX = left
            var textY = top - 15f

            if (textX < 10f)
                textX = 10f

            if (textX + textWidth > width)
                textX = width - textWidth - 10f

            if (textY < 60f)
                textY = top + 50f

            canvas.drawRoundRect(
                textX - 12f,
                textY - 42f,
                textX + textWidth + 12f,
                textY + 10f,
                12f,
                12f,
                bgPaint
            )

            canvas.drawText(
                label,
                textX,
                textY,
                textPaint
            )
        }
    }

    private fun getColor(classId: Int): Int {

        return when (classId % 8) {

            0 -> Color.GREEN
            1 -> Color.CYAN
            2 -> Color.YELLOW
            3 -> Color.RED
            4 -> Color.MAGENTA
            5 -> Color.BLUE
            6 -> Color.WHITE
            else -> Color.LTGRAY
        }
    }
}