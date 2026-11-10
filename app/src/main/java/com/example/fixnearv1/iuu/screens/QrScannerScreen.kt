package com.example.fixnearv1.iuu.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import androidx.compose.ui.unit.dp

@ExperimentalGetImage
@Composable
fun QrScannerScreen(onRegresar: () -> Unit = {}) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var qrText by remember {
        mutableStateOf("Escanea un código QR")
    }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = {}
        )

    LaunchedEffect(Unit) {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        AndroidView(
            factory = { ctx ->

                val previewView = PreviewView(ctx)

                val cameraProviderFuture =
                    ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({

                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build()

                    preview.setSurfaceProvider(
                        previewView.surfaceProvider
                    )

                    val imageAnalysis =
                        ImageAnalysis.Builder()
                            .build()

                    imageAnalysis.setAnalyzer(
                        Executors.newSingleThreadExecutor()
                    ) { imageProxy ->

                        val mediaImage = imageProxy.image

                        if (mediaImage != null) {

                            val buffer =
                                mediaImage.planes[0].buffer

                            val bytes =
                                ByteArray(buffer.remaining())

                            buffer.get(bytes)

                            val source = PlanarYUVLuminanceSource(
                                bytes,
                                mediaImage.width,
                                mediaImage.height,
                                0,
                                0,
                                mediaImage.width,
                                mediaImage.height,
                                false
                            )

                            val binaryBitmap = BinaryBitmap(
                                HybridBinarizer(source)
                            )

                            try {

                                val result = MultiFormatReader()
                                    .decode(binaryBitmap)

                                qrText = result.text

                            } catch (e: Exception) {
                            }

                        }

                        imageProxy.close()
                    }

                    val cameraSelector =
                        CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )

                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.weight(1f)
        )

        Text(
            text = qrText,
            modifier = Modifier.padding(16.dp)
        )
    }
}