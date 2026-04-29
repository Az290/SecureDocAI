package com.securedoc.ai.presentation.scanner

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
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
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("📸 Scan") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("📋 History ($documentCount)") })
        }

        when (selectedTab) {
            0 -> ScanTabContent(viewModel, context, lifecycleOwner, cameraPermission, imageCapture) { imageCapture = it }
            1 -> HistoryTabContent(viewModel, recentDocuments)
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

    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onScanStarted()
            recognizeFromUri(context, it) { text ->
                viewModel.onTextScanned(text)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(0.4f)) {
            if (cameraPermission.status.isGranted) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            val capture = ImageCapture.Builder().build()
                            imageCaptureRef = capture
                            onImageCaptureChange(capture)
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Cần quyền Camera", modifier = Modifier.align(Alignment.Center))
            }
        }

        Column(
            modifier = Modifier
                .weight(0.6f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Nút Scan và Chọn từ Thư viện
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        viewModel.onScanStarted()
                        captureAndRecognize(imageCaptureRef) { text -> viewModel.onTextScanned(text) }
                    },
                    enabled = !isProcessing && cameraPermission.status.isGranted,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("📸 Chụp ảnh")
                    }
                }

                FilledTonalButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    enabled = !isProcessing,
                    modifier = Modifier.height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🖼 Thư viện")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (error != null) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(" $error", color = Color.Red)
                        Button(onClick = { viewModel.retryClassification() }) { Text("Thử lại") }
                    }
                }
            }

            if (documentType.isNotEmpty() && error == null) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(" Loại: $documentType", style = MaterialTheme.typography.titleLarge)
                        LinearProgressIndicator(progress = { confidence.toFloat() }, modifier = Modifier.fillMaxWidth().height(8.dp))
                        Text("Độ tin cậy: ${(confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (summary.isNotEmpty() && error == null) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("📝 Thông tin trích xuất", style = MaterialTheme.typography.titleMedium)
                        Text(summary, style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.exportToPdf(context) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(" Lưu PDF")
                            }
                            Button(
                                onClick = { viewModel.shareResult(context) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Text(" Chia sẻ")
                            }
                        }
                    }
                }
            }

            if (scannedText.isNotEmpty()) {
                var showOriginal by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { showOriginal = !showOriginal }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showOriginal) "▲ Ẩn văn bản gốc" else "▼ Xem văn bản gốc")
                }
                if (showOriginal) {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                        Text(scannedText, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTabContent(viewModel: ScannerViewModel, recentDocuments: List<com.securedoc.ai.data.local.DocumentEntity>) {
    if (recentDocuments.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
            Text("Chưa có lịch sử scan nào", color = Color.Gray)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(recentDocuments) { doc ->
                HistoryCard(doc, viewModel)
            }
        }
    }
}

@Composable
fun HistoryCard(document: com.securedoc.ai.data.local.DocumentEntity, viewModel: ScannerViewModel) {
    var showDetails by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(document.documentType, style = MaterialTheme.typography.titleMedium)
                    Text(viewModel.formatDate(document.timestamp), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.clickable { viewModel.deleteDocument(document.id) }, tint = Color.Red)
            }
            if (showDetails) {
                Text(document.summary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }
            OutlinedButton(onClick = { showDetails = !showDetails }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(if (showDetails) "Ẩn chi tiết" else "Xem chi tiết")
            }
        }
    }
}

private fun captureAndRecognize(imageCapture: ImageCapture?, onResult: (String) -> Unit) {
    imageCapture?.takePicture(Executors.newSingleThreadExecutor(), object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees))
                .addOnSuccessListener { onResult(it.text.ifEmpty { "Không tìm thấy text" }); image.close() }
                .addOnFailureListener { onResult("Lỗi OCR"); image.close() }
        }
    })
}

private fun recognizeFromUri(context: android.content.Context, uri: Uri, onResult: (String) -> Unit) {
    try {
        val inputImage = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(inputImage)
            .addOnSuccessListener { onResult(it.text.ifEmpty { "Không tìm thấy text" }) }
            .addOnFailureListener { onResult("Lỗi OCR") }
    } catch (e: Exception) {
        onResult("Lỗi xử lý ảnh")
    }
}