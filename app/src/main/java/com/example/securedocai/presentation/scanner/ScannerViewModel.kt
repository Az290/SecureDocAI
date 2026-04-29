package com.securedoc.ai.presentation.scanner

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securedoc.ai.data.local.DocumentEntity
import com.securedoc.ai.data.local.LocalDocumentRepository
import com.securedoc.ai.data.remote.DocumentRepository
import com.securedoc.ai.utils.PdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val localRepository: LocalDocumentRepository,
    private val repository: DocumentRepository
) : ViewModel() {

    private val _scannedText = MutableStateFlow("")
    val scannedText: StateFlow<String> = _scannedText

    private val _documentType = MutableStateFlow("")
    val documentType: StateFlow<String> = _documentType

    private val _confidence = MutableStateFlow(0.0)
    val confidence: StateFlow<Double> = _confidence

    private val _summary = MutableStateFlow("")
    val summary: StateFlow<String> = _summary

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _serverHealthy = MutableStateFlow(false)
    val serverHealthy: StateFlow<Boolean> = _serverHealthy

    private val _recentDocuments = MutableStateFlow<List<DocumentEntity>>(emptyList())
    val recentDocuments: StateFlow<List<DocumentEntity>> = _recentDocuments

    private val _documentCount = MutableStateFlow(0)
    val documentCount: StateFlow<Int> = _documentCount

    init {
        checkServerHealth()
        loadRecentDocuments()
        loadDocumentCount()
    }

    private fun checkServerHealth() {
        viewModelScope.launch {
            try {
                _serverHealthy.value = repository.checkServerHealth()
            } catch (e: Exception) {
                _serverHealthy.value = false
            }
        }
    }

    fun onTextScanned(text: String) {
        _scannedText.value = text
        _error.value = null
        if (text.isNotEmpty() && text != "Không tìm thấy text") {
            processDocument(text)
        }
    }

    fun onScanStarted() {
        _documentType.value = ""
        _summary.value = ""
        _confidence.value = 0.0
        _error.value = null
        _isProcessing.value = true
    }

    private fun processDocument(text: String) {
        viewModelScope.launch {
            try {
                val classifyResult = repository.classifyDocument(text)
                _documentType.value = classifyResult.label
                _confidence.value = classifyResult.confidence

                val summary = repository.summarizeDocument(text, classifyResult.label)
                _summary.value = summary

                localRepository.saveDocument(
                    scannedText = text,
                    documentType = classifyResult.label,
                    confidence = classifyResult.confidence,
                    summary = summary
                )
                loadRecentDocuments()
                loadDocumentCount()

            } catch (e: Exception) {
                _error.value = "Lỗi: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun retryClassification() {
        if (_scannedText.value.isNotEmpty()) {
            _isProcessing.value = true
            processDocument(_scannedText.value)
        }
    }

    // --- HISTORY ---
    fun loadRecentDocuments() {
        viewModelScope.launch {
            _recentDocuments.value = localRepository.getRecentDocuments()
        }
    }

    fun loadDocumentCount() {
        viewModelScope.launch {
            _documentCount.value = localRepository.getDocumentCount()
        }
    }

    fun deleteDocument(id: Int) {
        viewModelScope.launch {
            localRepository.deleteDocument(id)
            loadRecentDocuments()
            loadDocumentCount()
        }
    }

    fun restoreFromHistory(document: DocumentEntity) {
        _scannedText.value = document.scannedText
        _documentType.value = document.documentType
        _confidence.value = document.confidence
        _summary.value = document.summary
    }

    fun formatDate(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val sdf = java.text.SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale("vi", "VN"))
        return sdf.format(date)
    }

    // --- EXPORT & SHARE ---
    fun exportToPdf(context: Context) {
        val success = PdfExporter.exportToPdf(
            context = context,
            documentType = _documentType.value,
            confidence = _confidence.value,
            scannedText = _scannedText.value,
            summary = _summary.value
        )
        if (success) {
            Toast.makeText(context, "Đã lưu PDF vào thư mục ẩn của App!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Lỗi khi lưu PDF", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareResult(context: Context) {
        val shareText = """
            BÁO CÁO PHÂN LOẠI TÀI LIỆU
            
            Loại: ${_documentType.value}
            Độ tin cậy: ${(_confidence.value * 100).toInt()}%
            
            Thông tin trích xuất:
            ${_summary.value}
            
            ~ Tạo bởi SecureDocAI ~
        """.trimIndent()

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua"))
    }
}