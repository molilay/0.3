package com.ultimatetool.app.di

import com.ultimatetool.app.data.repository.AuthRepositoryImpl
import com.ultimatetool.app.data.repository.ChatRepositoryImpl
import com.ultimatetool.app.data.repository.LinuxRepositoryImpl
import com.ultimatetool.app.data.repository.PluginsRepositoryImpl
import com.ultimatetool.app.domain.repository.AuthRepository
import com.ultimatetool.app.domain.repository.ChatRepository
import com.ultimatetool.app.domain.repository.LinuxRepository
import com.ultimatetool.app.domain.repository.PluginsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindPluginsRepository(impl: PluginsRepositoryImpl): PluginsRepository

    @Binds
    @Singleton
    abstract fun bindLinuxRepository(impl: LinuxRepositoryImpl): LinuxRepository
}
