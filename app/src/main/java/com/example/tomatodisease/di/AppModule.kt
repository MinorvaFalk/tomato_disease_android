package com.example.tomatodisease.di

import android.content.Context
import com.example.tomatodisease.api.API
import com.example.tomatodisease.domain.repository.DataStoreRepository
import com.example.tomatodisease.domain.repository.MainRepository
import com.example.tomatodisease.domain.repository.MainRepositoryImpl
import com.example.tomatodisease.domain.repository.MainRepositoryRetrofit
import com.example.tomatodisease.domain.service.MainService
import com.example.tomatodisease.domain.service.MainServiceImpl
import com.example.tomatodisease.domain.service.SocketService
import com.example.tomatodisease.domain.service.SocketServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDataStoreRepository(
        @ApplicationContext context: Context
    ): DataStoreRepository = DataStoreRepository(context)

    @Provides
    @Singleton
    fun provideMainService(
        @NetworkModule.HTTPClient
        httpClient: HttpClient,
    ): MainService = MainServiceImpl(
        httpClient,
    )

    @Provides
    @Singleton
    fun provideSocketService(
        @NetworkModule.SocketClient
        socketClient: HttpClient
    ): SocketService = SocketServiceImpl(
        socketClient
    )

    @Provides
    @Singleton
    fun provideMainRepository(
        api: API,
        @DispatchersModule.IoDispatcher
        dispatcher: CoroutineDispatcher
    ): MainRepository = MainRepositoryRetrofit(
        api,
        dispatcher
    )
//    fun provideMainRepository(
//        service: MainService,
//        @DispatchersModule.IoDispatcher
//        dispatcher: CoroutineDispatcher
//    ): MainRepository = MainRepositoryImpl(
//        service,
//        dispatcher
//    )



}