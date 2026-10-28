package com.synapse.auramirror

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Rational
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import kotlin.math.min

@Composable
fun CameraCaptureScreen(
    isAnalyzing: Boolean,
    capturedPreview: Bitmap?,
    isDemoPreviewMode: Boolean = false,
    lang: AppLang,
    onImageCaptured: (CapturedImage) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isCapturing by remember { mutableStateOf(false) }
    var isDisposed by remember { mutableStateOf(false) }
    val demoPreviewBitmap = remember(isDemoPreviewMode) {
        if (isDemoPreviewMode) {
            runCatching {
                context.assets.open("sample_face.jpg").use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        } else null
    }

    val previewUseCase = remember {
        Preview.Builder().build()
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    DisposableEffect(lifecycleOwner, isDemoPreviewMode) {
        isDisposed = false
        onDispose {
            isDisposed = true
            if (!isDemoPreviewMode) {
                try {
                    val providerFuture = ProcessCameraProvider.getInstance(context)
                    providerFuture.addListener({
                        try {
                            providerFuture.get().unbind(previewUseCase, imageCapture)
                        } catch (ignored: Exception) {
                        }
                    }, ContextCompat.getMainExecutor(context))
                } catch (ignored: Exception) {
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        // 1:1 Square Viewfinder container so live camera preview and captured 600x600 image match 100%
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .aspectRatio(1f)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(24.dp))
                .clipToBounds()
                .background(Color(0xFF1E293B))
        ) {
            if (isDemoPreviewMode && demoPreviewBitmap != null) {
                Image(
                    bitmap = demoPreviewBitmap.asImageBitmap(),
                    contentDescription = "Demo Sample Model Portrait Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (!isDemoPreviewMode) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = 1.35f,
                            scaleY = 1.35f
                        ),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            if (isDisposed) return@addListener
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                previewUseCase.setSurfaceProvider(previewView.surfaceProvider)
                                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                                val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
                                val viewPort = ViewPort.Builder(Rational(1, 1), rotation)
                                    .setScaleType(ViewPort.FILL_CENTER)
                                    .build()
                                val useCaseGroup = UseCaseGroup.Builder()
                                    .addUseCase(previewUseCase)
                                    .addUseCase(imageCapture)
                                    .setViewPort(viewPort)
                                    .build()

                                cameraProvider.unbind(previewUseCase, imageCapture)
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    useCaseGroup
                                )
                            } catch (exc: Exception) {
                                if (!isDisposed) {
                                    onError("Camera initialization failed: ${exc.message}")
                                }
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }
                )
            }

            if (capturedPreview != null) {
                Image(
                    bitmap = capturedPreview.asImageBitmap(),
                    contentDescription = "Captured Face Frozen Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x330F172A))
                )
            }

            // Luminous Skin AI Scanner Brackets + Face Oval Guide (78% width so face > 65% of 600x600 frame)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sqLeft = 2.dp.toPx()
                val sqTop = 2.dp.toPx()
                val sqRight = size.width - 2.dp.toPx()
                val sqBottom = size.height - 2.dp.toPx()
                val squareSide = size.width - 4.dp.toPx()

                drawRoundRect(
                    color = Color(0x6610B981),
                    topLeft = Offset(sqLeft, sqTop),
                    size = Size(squareSide, squareSide),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )

                val bracketLen = 30.dp.toPx()
                val bracketStroke = 4.dp.toPx()
                val bracketCol = Color(0xFF10B981)
                drawLine(bracketCol, Offset(sqLeft, sqTop), Offset(sqLeft + bracketLen, sqTop), bracketStroke, StrokeCap.Round)
                drawLine(bracketCol, Offset(sqLeft, sqTop), Offset(sqLeft, sqTop + bracketLen), bracketStroke, StrokeCap.Round)
                drawLine(bracketCol, Offset(sqRight, sqTop), Offset(sqRight - bracketLen, sqTop), bracketStroke, StrokeCap.Round)
                drawLine(bracketCol, Offset(sqRight, sqTop), Offset(sqRight, sqTop + bracketLen), bracketStroke, StrokeCap.Round)
                drawLine(bracketCol, Offset(sqLeft, sqBottom), Offset(sqLeft + bracketLen, sqBottom), bracketStroke, StrokeCap.Round)
                drawLine(bracketCol, Offset(sqLeft, sqBottom), Offset(sqLeft, sqBottom - bracketLen), bracketStroke, StrokeCap.Round)
                drawLine(bracketCol, Offset(sqRight, sqBottom), Offset(sqRight - bracketLen, sqBottom), bracketStroke, StrokeCap.Round)
                drawLine(bracketCol, Offset(sqRight, sqBottom), Offset(sqRight, sqBottom - bracketLen), bracketStroke, StrokeCap.Round)

                val ovalWidth = size.width * 0.78f
                val ovalHeight = size.height * 0.90f
                val ovalLeft = (size.width - ovalWidth) / 2f
                val ovalTop = (size.height - ovalHeight) / 2f

                drawOval(
                    color = Color(0xDDFFFFFF),
                    topLeft = Offset(ovalLeft, ovalTop),
                    size = Size(ovalWidth, ovalHeight),
                    style = Stroke(width = 2.8.dp.toPx())
                )
            }
        }

        // Framing & Privacy Hint Pill (or Captured Hold Confirmation)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 136.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val isHoldMode = isAnalyzing || capturedPreview != null
            Text(
                text = if (isHoldMode) AppI18n.capturedHoldBadge(lang) else AppI18n.cameraGuideHint(lang),
                color = if (isHoldMode) Color.White else Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier
                    .background(
                        if (isHoldMode) Color(0xF2059669) else Color(0xE6FFFFFF),
                        RoundedCornerShape(50)
                    )
                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Capture Controls Bar
        val canCapture = !isCapturing && !isAnalyzing
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isCapturing || isAnalyzing) {
                CircularProgressIndicator(
                    color = Color(0xFF10B981),
                    modifier = Modifier.size(56.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .background(Color(0xEBFFFFFF), RoundedCornerShape(50))
                        .border(1.5.dp, Color(0xFFDCE6F2), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Button(
                        onClick = {
                            if (!canCapture) return@Button
                            if (isDemoPreviewMode) {
                                try {
                                    val sampleBytes = context.assets.open("sample_face.jpg").use { it.readBytes() }
                                    onImageCaptured(CapturedImage(sampleBytes, 0, isSample = true))
                                } catch (e: Exception) {
                                    onError("Sample face load failed: ${e.message}")
                                }
                                return@Button
                            }
                            isCapturing = true
                            imageCapture.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        try {
                                            if (!isDisposed) {
                                                val rotation = image.imageInfo.rotationDegrees
                                                val bytes = imageProxyToByteArray(image)
                                                onImageCaptured(CapturedImage(bytes, rotation, isSample = false))
                                            }
                                        } catch (e: Exception) {
                                            if (!isDisposed) {
                                                onError("Image processing error: ${e.message}")
                                            }
                                        } finally {
                                            image.close()
                                            isCapturing = false
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        isCapturing = false
                                        if (!isDisposed) {
                                            onError("Capture failed: ${exception.message}")
                                        }
                                    }
                                }
                            )
                        },
                        enabled = canCapture,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            disabledContainerColor = Color.LightGray
                        )
                    ) {
                        Text(
                            text = AppI18n.scanMyFaceBtn(lang),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = {
                            if (!canCapture) return@OutlinedButton
                            try {
                                val sampleBytes = context.assets.open("sample_face.jpg").use { it.readBytes() }
                                onImageCaptured(CapturedImage(sampleBytes, 0, isSample = true))
                            } catch (e: Exception) {
                                onError("Sample face load failed: ${e.message}")
                            }
                        },
                        enabled = canCapture,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color(0xFF94A3B8))
                    ) {
                        Text(
                            text = AppI18n.demoSampleBtn(lang),
                            color = Color(0xFF475569),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

private fun imageProxyToByteArray(image: ImageProxy): ByteArray {
    val plane = image.planes[0]
    val buffer: ByteBuffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return bytes
}
