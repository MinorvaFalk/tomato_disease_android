package com.example.tomatodisease.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @DefaultDispatcher
    internal fun provideDefaultDispatcher(): CoroutineDispatcher =
        Dispatchers.Default

    @Provides
    @IoDispatcher
    internal fun provideIoDispatcher(): CoroutineDispatcher =
        Dispatchers.IO

    @Provides
    @MainDispatcher
    internal fun provideMainDispatcher(): CoroutineDispatcher =
        Dispatchers.Main

    @Qualifier
    annotation class DefaultDispatcher

    @Qualifier
    annotation class IoDispatcher

    @Qualifier
    annotation class MainDispatcher
}