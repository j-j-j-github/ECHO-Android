package com.example.echo.core.di

import com.example.echo.data.repository.EchoRepositoryImpl
import com.example.echo.domain.repository.EchoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindEchoRepository(
        impl: EchoRepositoryImpl
    ): EchoRepository
}