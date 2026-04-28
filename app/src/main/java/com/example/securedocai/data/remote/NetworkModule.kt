package com.securedoc.ai.data.remote

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("groq")
    fun provideGroqRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.groq.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    @Named("classifier")
    fun provideClassifierRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        // Emulator: 10.0.2.2 = localhost của máy host
        .baseUrl("http://10.0.2.2:5000/")
        // Device thật: thay bằng IP máy tính (VD: http://192.168.1.100:5000/)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideGroqApi(@Named("groq") retrofit: Retrofit): GroqApi =
        retrofit.create(GroqApi::class.java)

    @Provides
    @Singleton
    fun provideClassifierApi(@Named("classifier") retrofit: Retrofit): ClassifierApi =
        retrofit.create(ClassifierApi::class.java)
}