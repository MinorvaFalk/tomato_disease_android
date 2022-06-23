package com.example.tomatodisease.di

import com.example.tomatodisease.api.API
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    @HTTPClient
    fun provideKtorHttpClient(): HttpClient =
        HttpClient(OkHttp) {
            install(ContentNegotiation){
                gson {
                    setPrettyPrinting()
                    disableHtmlEscaping()
                }
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
            defaultRequest {
                url {
                    host = API.BASE_URL
                    contentType(ContentType.Application.Json)
                }
            }
        }

    @Provides
    @Singleton
    @SocketClient
    fun provideKtorSocketClient(): HttpClient =
        HttpClient(CIO) {
            install(WebSockets) {
                maxFrameSize = Long.MAX_VALUE
            }
        }

    @Provides
    @Singleton
    fun provideRetrofitClient(): API =
        Retrofit.Builder()
            .baseUrl(API.Endpoints.StillImages.url)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(API::class.java)

    @Qualifier
    annotation class HTTPClient

    @Qualifier
    annotation class SocketClient

}