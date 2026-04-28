package com.securedoc.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Query
import androidx.room.PrimaryKey
import java.util.Date

// Entity (Model dữ liệu)
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val scannedText: String,
    val documentType: String,
    val confidence: Double,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)

// DAO (Xử lý database)
@Dao
interface DocumentDao {
    @Insert
    suspend fun insertDocument(document: DocumentEntity)

    @Query("SELECT * FROM documents ORDER BY timestamp DESC LIMIT 20")
    suspend fun getRecentDocuments(): List<DocumentEntity>

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: Int)

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun getDocumentCount(): Int
}

// Database
@Database(entities = [DocumentEntity::class], version = 1)
abstract class DocumentDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: DocumentDatabase? = null

        fun getDatabase(context: Context): DocumentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DocumentDatabase::class.java,
                    "document_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}