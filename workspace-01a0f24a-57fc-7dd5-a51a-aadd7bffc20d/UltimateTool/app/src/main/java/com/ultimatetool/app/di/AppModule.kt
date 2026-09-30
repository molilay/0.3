package com.ultimatetool.app.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ultimatetool.app.data.local.AppDatabase
import com.ultimatetool.app.data.local.LinuxInstanceDao
import com.ultimatetool.app.data.local.MessageDao
import com.ultimatetool.app.data.local.PluginDao
import com.ultimatetool.app.linux.ProotEngine
import com.ultimatetool.app.plugin_engine.LuaPluginEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "ultimatetool.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    fun providePluginDao(db: AppDatabase): PluginDao = db.pluginDao()

    @Provides
    fun provideLinuxInstanceDao(db: AppDatabase): LinuxInstanceDao = db.linuxInstanceDao()

    @Provides
    @Singleton
    fun provideLuaPluginEngine(@ApplicationContext context: Context): LuaPluginEngine =
        LuaPluginEngine(context)

    @Provides
    @Singleton
    fun provideProotEngine(@ApplicationContext context: Context): ProotEngine =
        ProotEngine(context)
}
