package com.example.yoladetection

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class CameraAnalyzer(
    private val callback: (DetectionResponse) -> Unit
) : ImageAnalysis.Analyzer {

    private var busy = false

    override fun analyze(image: ImageProxy) {

        if (busy) {
            image.close()
            return
        }

        busy = true

        val bitmap = ImageUtils.imageProxyToBitmap(image)

        val stream = ByteArrayOutputStream()

        bitmap.compress(
            android.graphics.Bitmap.CompressFormat.JPEG,
            80,
            stream
        )

        val bytes = stream.toByteArray()

        val requestBody =
            bytes.toRequestBody("image/jpeg".toMediaType())

        val part =
            MultipartBody.Part.createFormData(
                "file",
                "frame.jpg",
                requestBody
            )

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val response = RetrofitClient.api.detect(part)

                response.body()?.let {
                    callback(it)
                }

            } catch (e: Exception) {

                e.printStackTrace()

            } finally {

                busy = false

                image.close()

            }

        }

    }

}