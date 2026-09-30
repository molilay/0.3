package com.ultimatetool.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val role: String,       // "user" | "assistant"
    val content: String,
    val createdAt: Long
)

@Entity(tableName = "plugins")
data class PluginEntity(
    @PrimaryKey val id: String,
    val name: String,
    val version: String,
    val source: String,     // Lua-код
    val enabled: Boolean,
    val updatedAt: Long
)

@Entity(tableName = "linux_instances")
data class LinuxInstanceEntity(
    @PrimaryKey val distroId: String,
    val state: String,          // installed | error
    val customUrl: String,      // переопределение URL rootfs (для экспериментальных)
    val vncPort: Int,
    val updatedAt: Long
)

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY createdAt DESC LIMIT :limit")
    suspend fun last(limit: Int): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MessageEntity)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()
}

@Dao
interface PluginDao {
    @Query("SELECT * FROM plugins ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PluginEntity>>

    @Query("SELECT COUNT(*) FROM plugins")
    fun count(): Flow<Int>

    @Query("SELECT * FROM plugins WHERE id = :id")
    suspend fun getById(id: String): PluginEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PluginEntity)

    @Query("DELETE FROM plugins WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface LinuxInstanceDao {
    @Query("SELECT * FROM linux_instances ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<LinuxInstanceEntity>>

    @Query("SELECT * FROM linux_instances WHERE distroId = :distroId")
    suspend fun getById(distroId: String): LinuxInstanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LinuxInstanceEntity)

    @Query("DELETE FROM linux_instances WHERE distroId = :distroId")
    suspend fun delete(distroId: String)
}

@Database(
    entities = [MessageEntity::class, PluginEntity::class, LinuxInstanceEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun pluginDao(): PluginDao
    abstract fun linuxInstanceDao(): LinuxInstanceDao
}
