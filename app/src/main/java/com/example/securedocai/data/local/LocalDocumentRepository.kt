package com.securedoc.ai.data.local

import javax.inject.Inject

class LocalDocumentRepository @Inject constructor(
    private val documentDao: DocumentDao
) {

    suspend fun saveDocument(
        scannedText: String,
        documentType: String,
        confidence: Double,
        summary: String
    ) {
        val entity = DocumentEntity(
            scannedText = scannedText,
            documentType = documentType,
            confidence = confidence,
            summary = summary
        )
        documentDao.insertDocument(entity)
    }

    suspend fun getRecentDocuments(): List<DocumentEntity> {
        return documentDao.getRecentDocuments()
    }

    suspend fun deleteDocument(id: Int) {
        documentDao.deleteDocument(id)
    }

    suspend fun getDocumentCount(): Int {
        return documentDao.getDocumentCount()
    }
}