package com.securedoc.ai.presentation.scanner

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(viewModel: ScannerViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val scannedText by viewModel.scannedText.collectAsState()
    val documentType by viewModel.documentType.collectAsState()
    val confidence by viewModel.confidence.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val error by viewModel.error.collectAsState()
    val serverHealthy by viewModel.serverHealthy.collectAsState()
    val recentDocuments by viewModel.recentDocuments.collectAsState()
    val documentCount by viewModel.documentCount.collectAsState()

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Bar
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("📸 Scan") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("📋 History (${documentCount})") }
            )
        }

        // Content
        when (selectedTab) {
            0 -> ScanTabContent(
                viewModel = viewModel,
                context = context,
                lifecycleOwner = lifecycleOwner,
                cameraPermission = cameraPermission,
                imageCapture = imageCapture,
                onImageCaptureChange = { imageCapture = it }
            )
            1 -> HistoryTabContent(
                viewModel = viewModel,
                recentDocuments = recentDocuments
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanTabContent(
    viewModel: ScannerViewModel,
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraPermission: com.google.accompanist.permissions.PermissionState,
    imageCapture: ImageCapture?,
    onImageCaptureChange: (ImageCapture?) -> Unit
) {
    val scannedText by viewModel.scannedText.collectAsState()
    val documentType by viewModel.documentType.collectAsState()
    val confidence by viewModel.confidence.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val error by viewModel.error.collectAsState()
    val serverHealthy by viewModel.serverHealthy.collectAsState()

    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Server Status Bar
        AnimatedVisibility(
            visible = !serverHealthy,
            enter = slideInVertically(),
            exit = slideOutVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Text(
                    "⚠️ Python server chưa kết nối",
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFFC62828)
                )
            }
        }

        // Camera Preview
        Box(modifier = Modifier.weight(0.4f)) {
            if (cameraPermission.status.isGranted) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                .build()

                            imageCaptureRef = imageCapture
                            onImageCaptureChange(imageCapture)

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    "Cần quyền Camera để scan tài liệu",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Results & Actions
        Column(
            modifier = Modifier
                .weight(0.6f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scan Button
            Button(
                onClick = {
                    viewModel.onScanStarted()
                    captureAndRecognize(imageCaptureRef) { text ->
                        viewModel.onTextScanned(text)
                    }
                },
                enabled = !isProcessing && cameraPermission.status.isGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                AnimatedContent(
                    targetState = isProcessing,
                    label = "button_animation"
                ) { processing ->
                    if (processing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("⏳ Đang xử lý...")
                    } else {
                        Text("📸 Scan tài liệu")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error Display
            AnimatedVisibility(
                visible = error != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                error?.let {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("❌ Lỗi", style = MaterialTheme.typography.titleMedium)
                            Text(it, color = Color(0xFFC62828))
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.retryClassification() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Thử lại")
                            }
                        }
                    }
                }
            }

            // Classification Result
            AnimatedVisibility(
                visible = documentType.isNotEmpty() && error == null,
                enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "📂 Phân loại",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = documentType,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { confidence.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Độ tin cậy: ${(confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary
            AnimatedVisibility(
                visible = summary.isNotEmpty() && error == null,
                enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "📝 Thông tin trích xuất",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            summary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 10,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Original Text (Expandable)
            if (scannedText.isNotEmpty()) {
                var showOriginal by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showOriginal = !showOriginal },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showOriginal) "▲ Ẩn văn bản gốc" else "▼ Xem văn bản gốc")
                }

                AnimatedVisibility(
                    visible = showOriginal,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            scannedText,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTabContent(
    viewModel: ScannerViewModel,
    recentDocuments: List<com.securedoc.ai.data.local.DocumentEntity>
) {
    if (recentDocuments.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "No history",
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Chưa có scan nào",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recentDocuments) { document ->
                HistoryCard(
                    document = document,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun HistoryCard(
    document: com.securedoc.ai.data.local.DocumentEntity,
    viewModel: ScannerViewModel
) {
    var showDetails by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .background(
                color = when (document.documentType) {
                    "HÓA ĐƠN" -> Color(0xFFE3F2FD)
                    "HỢP ĐỒNG" -> Color(0xFFF3E5F5)
                    "CMND" -> Color(0xFFE8F5E9)
                    else -> Color(0xFFFFF3E0)
                },
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        document.documentType,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        viewModel.formatDate(document.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        "${(document.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { viewModel.deleteDocument(document.id) },
                        tint = Color(0xFFC62828)
                    )
                }
            }

            // Expandable Details
            if (showDetails) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "📋 Văn bản gốc:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    document.scannedText.take(150) + if (document.scannedText.length > 150) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "📝 Tóm tắt:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    document.summary.take(150) + if (document.summary.length > 150) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.restoreFromHistory(document) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Xem chi tiết")
                }
            }

            // Toggle Details
            OutlinedButton(
                onClick = { showDetails = !showDetails },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (showDetails) "▲ Ẩn chi tiết" else "▼ Xem chi tiết")
            }
        }
    }
}

private fun captureAndRecognize(
    imageCapture: ImageCapture?,
    onResult: (String) -> Unit
) {
    val executor = Executors.newSingleThreadExecutor()
    imageCapture?.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            try {
                val inputImage = InputImage.fromMediaImage(
                    image.image!!,
                    image.imageInfo.rotationDegrees
                )
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(inputImage)
                    .addOnSuccessListener { result ->
                        val text = result.text.ifEmpty { "Không tìm thấy text" }
                        onResult(text)
                        image.close()
                    }
                    .addOnFailureListener {
                        onResult("Lỗi OCR: ${it.message}")
                        image.close()
                    }
            } catch (e: Exception) {
                onResult("Lỗi: ${e.message}")
                image.close()
            }
        }

        override fun onError(exception: ImageCaptureException) {
            onResult("Lỗi camera: ${exception.message}")
        }
    })
}