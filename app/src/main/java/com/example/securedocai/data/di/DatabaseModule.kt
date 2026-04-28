package com.securedoc.ai.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.securedoc.ai.data.local.DocumentDatabase
import com.securedoc.ai.data.local.DocumentDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): DocumentDatabase {
        return DocumentDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideDocumentDao(database: DocumentDatabase): DocumentDao {
        return database.documentDao()
    }
}