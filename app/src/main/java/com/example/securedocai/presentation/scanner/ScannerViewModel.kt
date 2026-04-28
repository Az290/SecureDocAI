package com.securedoc.ai.presentation.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securedoc.ai.data.local.DocumentEntity
import com.securedoc.ai.data.local.LocalDocumentRepository
import com.securedoc.ai.data.remote.DocumentRepository
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

    // UI States - Current Scan
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

    // History States
    private val _recentDocuments = MutableStateFlow<List<DocumentEntity>>(emptyList())
    val recentDocuments: StateFlow<List<DocumentEntity>> = _recentDocuments

    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory

    private val _documentCount = MutableStateFlow(0)
    val documentCount: StateFlow<Int> = _documentCount

    init {
        checkServerHealth()
        loadRecentDocuments()
        loadDocumentCount()
    }

    // ==================== SERVER HEALTH ====================
    private fun checkServerHealth() {
        viewModelScope.launch {
            try {
                _serverHealthy.value = repository.checkServerHealth()
            } catch (e: Exception) {
                _serverHealthy.value = false
            }
        }
    }

    // ==================== CURRENT SCAN ====================
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
    }

    private fun processDocument(text: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null

            try {
                // Bước 1: Phân loại bằng ML
                android.util.Log.d("Scanner", " Đang phân loại...")
                val classifyResult = repository.classifyDocument(text)

                _documentType.value = classifyResult.label
                _confidence.value = classifyResult.confidence

                android.util.Log.d(
                    "Scanner",
                    "✅ Loại: ${classifyResult.label} (${classifyResult.confidence * 100}%)"
                )

                // Bước 2: Tóm tắt bằng Groq
                android.util.Log.d("Scanner", " Đang tóm tắt...")
                val summary = repository.summarizeDocument(text, classifyResult.label)
                _summary.value = summary

                android.util.Log.d("Scanner", " Tóm tắt xong")

                // Bước 3: Lưu vào history
                android.util.Log.d("Scanner", " Đang lưu...")
                localRepository.saveDocument(
                    scannedText = text,
                    documentType = classifyResult.label,
                    confidence = classifyResult.confidence,
                    summary = summary
                )
                android.util.Log.d("Scanner", " Lưu xong")

                // Reload history
                loadRecentDocuments()
                loadDocumentCount()

            } catch (e: Exception) {
                android.util.Log.e("Scanner", " Error: ${e.message}", e)
                _error.value = when {
                    e.message?.contains("Unable to resolve host") == true ->
                        " Không kết nối Python server. Kiểm tra server đang chạy?"
                    e.message?.contains("timeout") == true ->
                        " Server phản hồi quá lâu"
                    else ->
                        " Lỗi: ${e.message}"
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun retryClassification() {
        if (_scannedText.value.isNotEmpty()) {
            processDocument(_scannedText.value)
        }
    }

    // ==================== HISTORY MANAGEMENT ====================
    fun loadRecentDocuments() {
        viewModelScope.launch {
            _isLoadingHistory.value = true
            try {
                val documents = localRepository.getRecentDocuments()
                _recentDocuments.value = documents
                android.util.Log.d("Scanner", " Loaded ${documents.size} recent documents")
            } catch (e: Exception) {
                android.util.Log.e("Scanner", "Error loading documents: ${e.message}")
            } finally {
                _isLoadingHistory.value = false
            }
        }
    }

    fun loadDocumentCount() {
        viewModelScope.launch {
            try {
                val count = localRepository.getDocumentCount()
                _documentCount.value = count
                android.util.Log.d("Scanner", "Total documents: $count")
            } catch (e: Exception) {
                android.util.Log.e("Scanner", "Error loading count: ${e.message}")
            }
        }
    }

    fun deleteDocument(id: Int) {
        viewModelScope.launch {
            try {
                localRepository.deleteDocument(id)
                loadRecentDocuments()
                loadDocumentCount()
                android.util.Log.d("Scanner", "🗑 Deleted document $id")
            } catch (e: Exception) {
                android.util.Log.e("Scanner", "Error deleting document: ${e.message}")
            }
        }
    }

    fun restoreFromHistory(document: DocumentEntity) {
        _scannedText.value = document.scannedText
        _documentType.value = document.documentType
        _confidence.value = document.confidence
        _summary.value = document.summary
    }

    // ==================== UTILITY ====================
    fun formatDate(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val sdf = java.text.SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale("vi", "VN"))
        return sdf.format(date)
    }

    fun clearCurrentScan() {
        _scannedText.value = ""
        _documentType.value = ""
        _confidence.value = 0.0
        _summary.value = ""
        _error.value = null
    }
}