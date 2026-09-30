package com.ultimatetool.app.data.repository

import com.ultimatetool.app.data.local.LinuxInstanceDao
import com.ultimatetool.app.data.local.LinuxInstanceEntity
import com.ultimatetool.app.domain.model.LinuxInstance
import com.ultimatetool.app.domain.repository.LinuxRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LinuxRepositoryImpl @Inject constructor(
    private val dao: LinuxInstanceDao
) : LinuxRepository {

    override fun observeInstances(): Flow<List<LinuxInstance>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getInstance(distroId: String): LinuxInstance? =
        try {
            dao.getById(distroId)?.toDomain()
        } catch (_: Exception) {
            null
        }

    override suspend fun upsert(instance: LinuxInstance) {
        try {
            dao.upsert(
                LinuxInstanceEntity(
                    distroId = instance.distroId,
                    state = instance.state.take(20),
                    customUrl = instance.customUrl.take(2_000),
                    vncPort = instance.vncPort,
                    updatedAt = instance.updatedAt
                )
            )
        } catch (_: Exception) {
        }
    }

    override suspend fun delete(distroId: String) {
        try {
            dao.delete(distroId)
        } catch (_: Exception) {
        }
    }

    private fun LinuxInstanceEntity.toDomain() = LinuxInstance(
        distroId = distroId,
        state = state,
        customUrl = customUrl,
        vncPort = vncPort,
        updatedAt = updatedAt
    )
}
