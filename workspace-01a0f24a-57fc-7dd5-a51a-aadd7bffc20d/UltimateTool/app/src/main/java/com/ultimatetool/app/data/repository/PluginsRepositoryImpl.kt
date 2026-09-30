package com.ultimatetool.app.data.repository

import com.ultimatetool.app.data.local.PluginDao
import com.ultimatetool.app.data.local.PluginEntity
import com.ultimatetool.app.domain.model.PluginInfo
import com.ultimatetool.app.domain.repository.PluginsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PluginsRepositoryImpl @Inject constructor(
    private val pluginDao: PluginDao
) : PluginsRepository {

    override fun observePlugins(): Flow<List<PluginInfo>> =
        pluginDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun countPlugins(): Flow<Int> = pluginDao.count()

    override suspend fun getById(id: String): PluginInfo? =
        try {
            pluginDao.getById(id)?.toDomain()
        } catch (_: Exception) {
            null
        }

    override suspend fun upsert(plugin: PluginInfo) {
        try {
            pluginDao.upsert(
                PluginEntity(
                    id = plugin.id,
                    name = plugin.name.take(100),
                    version = plugin.version.take(20),
                    source = plugin.source.take(200_000),
                    enabled = plugin.enabled,
                    updatedAt = plugin.updatedAt
                )
            )
        } catch (_: Exception) {
        }
    }

    override suspend fun delete(id: String) {
        try {
            pluginDao.delete(id)
        } catch (_: Exception) {
        }
    }

    private fun PluginEntity.toDomain() = PluginInfo(
        id = id,
        name = name,
        version = version,
        source = source,
        enabled = enabled,
        updatedAt = updatedAt
    )
}
