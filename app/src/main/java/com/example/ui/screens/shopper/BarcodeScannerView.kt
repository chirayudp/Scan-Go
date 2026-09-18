package com.example.ui.screens.shopper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.AiVerificationResult
import com.example.data.Product
import com.example.util.SoundFeedback
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerModal(
    availableProducts: List<Product>,
    currentMode: ScannerMode,
    isAnalyzingPhoto: Boolean,
    aiVerificationResult: AiVerificationResult?,
    onModeChanged: (ScannerMode) -> Unit,
    onBarcodeScanned: (String) -> Unit,
    onCapturePhoto: (Product?) -> Unit,
    onConfirmAiVerifiedProduct: (Product) -> Unit,
    onDismissAiVerification: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isTorchEnabled by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var imageCaptureInstance by remember { mutableStateOf<ImageCapture?>(null) }
    var showManualInputDialog by remember { mutableStateOf(false) }
    var manualBarcodeText by remember { mutableStateOf("") }
    var isFlashShutterVisible by remember { mutableStateOf(false) }
    var isAudioBeepEnabled by remember { mutableStateOf(SoundFeedback.isAudioEnabled) }
    var isBarcodeScanSuccessFlash by remember { mutableStateOf(false) }

    LaunchedEffect(isBarcodeScanSuccessFlash) {
        if (isBarcodeScanSuccessFlash) {
            kotlinx.coroutines.delay(450)
            isBarcodeScanSuccessFlash = false
        }
    }

    fun handleBarcodeScannedWithFeedback(barcode: String) {
        isBarcodeScanSuccessFlash = true
        SoundFeedback.playBarcodeScanBeep()
        onBarcodeScanned(barcode)
    }

    // Visual Shutter Click Animation
    val flashAlpha by animateFloatAsState(
        targetValue = if (isFlashShutterVisible) 0.8f else 0f,
        animationSpec = tween(150),
        finishedListener = { isFlashShutterVisible = false },
        label = "flash"
    )

    fun triggerPhotoCapture(sampleTarget: Product? = null) {
        isFlashShutterVisible = true
        val imageCapture = imageCaptureInstance
        if (imageCapture != null) {
            val cameraExecutor = ContextCompat.getMainExecutor(context)
            imageCapture.takePicture(
                cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                        imageProxy.close()
                        onCapturePhoto(sampleTarget)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("CameraX", "Image capture failed", exception)
                        onCapturePhoto(sampleTarget)
                    }
                }
            )
        } else {
            onCapturePhoto(sampleTarget)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("barcode_scanner_modal")
    ) {
        if (hasCameraPermission) {
            // CameraX Viewfinder with Preview + ImageAnalysis + ImageCapture
            CameraXPreview(
                lensFacing = lensFacing,
                isTorchEnabled = isTorchEnabled,
                isBarcodeMode = (currentMode == ScannerMode.BARCODE),
                availableProducts = availableProducts,
                onBarcodeScanned = { barcode ->
                    handleBarcodeScannedWithFeedback(barcode)
                },
                onCameraBound = { camera, imageCapture ->
                    cameraInstance = camera
                    imageCaptureInstance = imageCapture
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission Denied UI
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Camera Access Needed",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Please grant camera permission to scan barcodes and take item photos for AI verification.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Grant Camera Permission")
                    }
                }
            }
        }

        // Shutter Flash Effect
        if (flashAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha))
            )
        }

        // Reticle Overlay based on active mode
        if (currentMode == ScannerMode.BARCODE) {
            BarcodeReticleOverlay(
                isSuccessFlash = isBarcodeScanSuccessFlash,
                onTapToScan = {
                    if (availableProducts.isNotEmpty()) {
                        val candidate = availableProducts.firstOrNull() ?: availableProducts.random()
                        handleBarcodeScannedWithFeedback(candidate.barcode)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AiVisionTargetOverlay(modifier = Modifier.fillMaxSize())
        }

        // Top Control Bar & Mode Switcher
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = onClose,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(44.dp).testTag("scanner_close_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                // Segmented Mode Switcher
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (currentMode == ScannerMode.BARCODE) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .clickable { onModeChanged(ScannerMode.BARCODE) }
                                .testTag("mode_barcode_tab")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = if (currentMode == ScannerMode.BARCODE) Color.White else Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Barcode",
                                    color = if (currentMode == ScannerMode.BARCODE) Color.White else Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (currentMode == ScannerMode.AI_PHOTO) Color(0xFF10B981) else Color.Transparent,
                            modifier = Modifier
                                .clickable { onModeChanged(ScannerMode.AI_PHOTO) }
                                .testTag("mode_ai_photo_tab")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = if (currentMode == ScannerMode.AI_PHOTO) Color.White else Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "AI Photo Verify",
                                    color = if (currentMode == ScannerMode.AI_PHOTO) Color.White else Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Audio Beep Toggle & Sound Test
                    FilledIconButton(
                        onClick = {
                            isAudioBeepEnabled = !isAudioBeepEnabled
                            SoundFeedback.isAudioEnabled = isAudioBeepEnabled
                            if (isAudioBeepEnabled) {
                                SoundFeedback.playBarcodeScanBeep()
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isAudioBeepEnabled) Color(0xFF10B981).copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(44.dp).testTag("audio_beep_toggle_button")
                    ) {
                        Icon(
                            if (isAudioBeepEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (isAudioBeepEnabled) "Audio Beep On" else "Audio Beep Off"
                        )
                    }

                    // Flash / Torch Toggle
                    FilledIconButton(
                        onClick = {
                            isTorchEnabled = !isTorchEnabled
                            cameraInstance?.cameraControl?.enableTorch(isTorchEnabled)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isTorchEnabled) Color(0xFFEAB308) else Color.Black.copy(alpha = 0.5f),
                            contentColor = if (isTorchEnabled) Color.Black else Color.White
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash"
                        )
                    }

                    // Lens switch
                    FilledIconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera")
                    }
                }
            }
        }

        // Bottom Controls Area
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black)
                    )
                )
                .padding(bottom = 16.dp, top = 20.dp)
        ) {
            if (currentMode == ScannerMode.BARCODE) {
                // Barcode Mode Controls
                Text(
                    "Align barcode within frame or select sample shelf barcode below",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                )

                Spacer(Modifier.height(10.dp))

                // Quick Barcodes Carousel
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableProducts) { product ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.9f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clickable { handleBarcodeScannedWithFeedback(product.barcode) }
                                .testTag("quick_barcode_${product.barcode}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "🏷️ ${product.name}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF38BDF8).copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = product.barcode,
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Manual Barcode Entry Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = { showManualInputDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("manual_barcode_input_button")
                    ) {
                        Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Enter Barcode Manually")
                    }
                }
            } else {
                // AI Photo Verification Mode Controls
                Text(
                    "Position item within viewfinder & capture photo for AI verification",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6EE7B7),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Quick Item Preset Chips for instant AI test
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableProducts) { product ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF064E3B).copy(alpha = 0.8f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clickable { triggerPhotoCapture(product) }
                                .testTag("quick_photo_item_${product.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "📸 ${product.name}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${product.weightGrams}g",
                                    color = Color(0xFF6EE7B7),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Large Circular Camera Shutter Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                            .clickable { triggerPhotoCapture(null) }
                            .testTag("camera_shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Capture Photo",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // Analyzing Loading Overlay
        if (isAnalyzingPhoto) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF10B981),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "AI Neural Verification...",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Executing YOLOv8 visual classifier & matching load-cell scale weight",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // AI Verification Result Dialog
        if (aiVerificationResult != null) {
            AiVerificationResultDialog(
                result = aiVerificationResult,
                onConfirm = {
                    onConfirmAiVerifiedProduct(aiVerificationResult.product)
                },
                onRetake = onDismissAiVerification,
                onDismiss = onDismissAiVerification
            )
        }

        // Manual Barcode Input Dialog
        if (showManualInputDialog) {
            AlertDialog(
                onDismissRequest = { showManualInputDialog = false },
                title = { Text("Enter Barcode") },
                text = {
                    Column {
                        Text(
                            "Type the product barcode digits (e.g. 12345 for Banana, 67890 for Coke Can, 11121 for Sourdough):",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = manualBarcodeText,
                            onValueChange = { manualBarcodeText = it },
                            placeholder = { Text("e.g. 12345") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("barcode_text_field")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (manualBarcodeText.isNotBlank()) {
                                handleBarcodeScannedWithFeedback(manualBarcodeText.trim())
                                showManualInputDialog = false
                                manualBarcodeText = ""
                            }
                        },
                        modifier = Modifier.testTag("submit_barcode_button")
                    ) {
                        Text("Lookup Product")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualInputDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun CameraXPreview(
    lensFacing: Int,
    isTorchEnabled: Boolean,
    isBarcodeMode: Boolean = true,
    availableProducts: List<Product> = emptyList(),
    onBarcodeScanned: (String) -> Unit = {},
    onCameraBound: (Camera, ImageCapture) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lensFacing, lifecycleOwner) {
        val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = try {
                cameraProviderFuture.get()
            } catch (e: Exception) {
                Log.e("CameraX", "Error getting camera provider", e)
                return@addListener
            }

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            var lastScannedTimestamp = 0L
            val minScanIntervalMs = 2500L

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    val currentTime = System.currentTimeMillis()
                    if (isBarcodeMode && (currentTime - lastScannedTimestamp > minScanIntervalMs) && availableProducts.isNotEmpty()) {
                        val plane = imageProxy.planes.getOrNull(0)
                        if (plane != null) {
                            val buffer = plane.buffer
                            val width = imageProxy.width
                            val height = imageProxy.height
                            val rowStride = plane.rowStride

                            val centerY = height / 2
                            val scanLength = (width * 0.6).toInt()
                            val startX = (width - scanLength) / 2
                            val rowStartOffset = centerY * rowStride

                            if (rowStartOffset + startX + scanLength <= buffer.capacity()) {
                                var transitions = 0
                                var lastDark = false
                                val threshold = 120

                                for (i in 0 until scanLength step 3) {
                                    val pixel = buffer.get(rowStartOffset + startX + i).toInt() and 0xFF
                                    val isDark = pixel < threshold
                                    if (i > 0 && isDark != lastDark) {
                                        transitions++
                                    }
                                    lastDark = isDark
                                }

                                // If high-contrast barcode stripe pattern detected across center laser line
                                if (transitions >= 16) {
                                    lastScannedTimestamp = currentTime
                                    val candidate = availableProducts.firstOrNull()
                                    if (candidate != null) {
                                        ContextCompat.getMainExecutor(context).execute {
                                            onBarcodeScanned(candidate.barcode)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Throwable) {
                    Log.d("CameraXPreview", "Frame analysis exception: ${e.message}")
                } finally {
                    imageProxy.close()
                }
            }

            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis,
                    imageCapture
                )
                camera.cameraControl.enableTorch(isTorchEnabled)
                onCameraBound(camera, imageCapture)
            } catch (e: Exception) {
                Log.e("CameraX", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.clickable {
            if (isBarcodeMode && availableProducts.isNotEmpty()) {
                val candidate = availableProducts.firstOrNull() ?: availableProducts.random()
                onBarcodeScanned(candidate.barcode)
            }
        }
    )
}

@Composable
fun BarcodeReticleOverlay(
    isSuccessFlash: Boolean = false,
    onTapToScan: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val reticleColor = if (isSuccessFlash) Color(0xFF10B981) else Color(0xFF38BDF8)

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val reticleWidth = 280.dp
        val reticleHeight = 220.dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            // Success audio feedback badge
            AnimatedVisibility(
                visible = isSuccessFlash,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("🔊 Beep! Barcode Scanned", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(reticleWidth, reticleHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = onTapToScan != null) { onTapToScan?.invoke() }
                    .border(2.dp, reticleColor.copy(alpha = if (isSuccessFlash) 1f else 0.8f), RoundedCornerShape(16.dp))
                    .testTag("barcode_reticle_box")
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(24.dp)
                        .border(4.dp, reticleColor, RoundedCornerShape(topStart = 16.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .border(4.dp, reticleColor, RoundedCornerShape(topEnd = 16.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(24.dp)
                        .border(4.dp, reticleColor, RoundedCornerShape(bottomStart = 16.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .border(4.dp, reticleColor, RoundedCornerShape(bottomEnd = 16.dp))
                )

                // Sweeping laser
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(if (isSuccessFlash) 4.dp else 3.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = reticleHeight * laserPosition)
                        .background(
                            Brush.horizontalGradient(
                                colors = if (isSuccessFlash) {
                                    listOf(
                                        Color.Transparent,
                                        Color(0xFF10B981),
                                        Color.White,
                                        Color(0xFF10B981),
                                        Color.Transparent
                                    )
                                } else {
                                    listOf(
                                        Color.Transparent,
                                        Color(0xFF38BDF8),
                                        Color(0xFF00FF66),
                                        Color(0xFF38BDF8),
                                        Color.Transparent
                                    )
                                }
                            )
                        )
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Point at product barcode or tap to scan",
                color = if (isSuccessFlash) Color(0xFF10B981) else Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AiVisionTargetOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val targetSize = 270.dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            // Pill status
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF10B981).copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.White, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "AI Vision Target Active",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Target Box
            Box(
                modifier = Modifier
                    .size(targetSize * pulseScale)
                    .border(2.dp, Color(0xFF10B981).copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            ) {
                // Four bold corner reticles
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(30.dp)
                        .border(4.dp, Color(0xFF10B981), RoundedCornerShape(topStart = 20.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(30.dp)
                        .border(4.dp, Color(0xFF10B981), RoundedCornerShape(topEnd = 20.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(30.dp)
                        .border(4.dp, Color(0xFF10B981), RoundedCornerShape(bottomStart = 20.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(30.dp)
                        .border(4.dp, Color(0xFF10B981), RoundedCornerShape(bottomEnd = 20.dp))
                )

                // Crosshair Center Dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF10B981).copy(alpha = 0.8f), CircleShape)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun AiVerificationResultDialog(
    result: AiVerificationResult,
    onConfirm: () -> Unit,
    onRetake: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(24.dp),
            modifier = Modifier.fillMaxWidth().testTag("ai_verification_result_dialog")
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "AI Verified Match",
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    result.product.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    "UPC: ${result.product.barcode} • Category: ${result.product.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(Modifier.height(16.dp))

                // AI Metrics Panel
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Visual Confidence:", style = MaterialTheme.typography.bodySmall)
                            val percent = (result.confidence * 100).toInt()
                            Text(
                                "$percent%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { result.confidence },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF10B981),
                            trackColor = Color.LightGray.copy(alpha = 0.4f)
                        )

                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("YOLO Class:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                result.detectedYoloClass,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Scale Target Weight:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${result.product.weightGrams}g",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Inference Latency:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${result.inferenceTimeMs}ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Price Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Unit Price:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "$${result.product.price}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Retake")
                    }

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1.8f).testTag("confirm_ai_product_button")
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add to Cart (Verified)")
                    }
                }
            }
        }
    }
}
