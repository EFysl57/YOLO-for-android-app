package com.example.yoladetection

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView

    private val cameraExecutor = Executors.newSingleThreadExecutor()


    private val busy = AtomicBoolean(false)

    companion object {
        private const val REQUEST_CAMERA = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA
            )
        }
    }

    private fun startCamera() {

        val providerFuture =
            ProcessCameraProvider.getInstance(this)

        providerFuture.addListener({

            val cameraProvider = providerFuture.get()

            val preview = Preview.Builder()
                .build()

            preview.setSurfaceProvider(previewView.surfaceProvider)

            val imageAnalysis =
                ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()

            imageAnalysis.setAnalyzer(
                cameraExecutor
            ) { image ->

                if (busy.get()) {
                    image.close()
                    return@setAnalyzer
                }

                busy.set(true)

                processImage(image)
            }

            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(image: ImageProxy) {

        val bitmap = imageProxyToBitmap(image)

        image.close()

        if (bitmap == null) {
            busy.set(false)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val stream =
                    ByteArrayOutputStream()

                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    80,
                    stream
                )

                val body =
                    stream.toByteArray()

                val requestBody =
                    body.toRequestBody(
                        "image/jpeg".toMediaType()
                    )

                val part =
                    MultipartBody.Part.createFormData(
                        "image",
                        "frame.jpg",
                        requestBody
                    )

                val response =
                    RetrofitClient.api.detect(part)

                if (response.isSuccessful) {

                    val detections =
                        response.body()?.detections ?: emptyList()

                    launch(Dispatchers.Main) {

                        overlayView.updateDetections(
                            detections,
                            bitmap.width,
                            bitmap.height
                        )
                    }
                }

            } finally {

                busy.set(false)
            }
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            image.width,
            image.height,
            null
        )

        val stream = ByteArrayOutputStream()

        yuvImage.compressToJpeg(
            Rect(
                0,
                0,
                image.width,
                image.height
            ),
            100,
            stream
        )

        val bytes = stream.toByteArray()

        var bitmap =
            BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size
            )


        val matrix = android.graphics.Matrix()

        matrix.postRotate(
            image.imageInfo.rotationDegrees.toFloat()
        )

        bitmap = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )


        bitmap = Bitmap.createScaledBitmap(
            bitmap,
            640,
            640,
            true
        )

        return bitmap
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == REQUEST_CAMERA &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        }
    }

    override fun onDestroy() {

        super.onDestroy()

        cameraExecutor.shutdown()
    }
}