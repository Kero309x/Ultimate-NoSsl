
package com.ultimate.nossl.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HookLogDao {
    @Query("SELECT * FROM hook_logs ORDER BY timestamp DESC LIMIT 500")
    fun getAllLogs(): Flow<List<HookLogEntity>>

    @Query("SELECT COUNT(*) FROM hook_logs")
    fun getLogCount(): Flow<Int>

    @Query("SELECT * FROM hook_logs WHERE level = :level ORDER BY timestamp DESC LIMIT 200")
    fun getLogsByLevel(level: String): Flow<List<HookLogEntity>>

    @Query("SELECT * FROM hook_logs WHERE tag LIKE '%' || :query || '%' OR message LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT 200")
    fun searchLogs(query: String): Flow<List<HookLogEntity>>

    @Insert
    suspend fun insertLog(log: HookLogEntity)

    @Query("DELETE FROM hook_logs WHERE id NOT IN (SELECT id FROM hook_logs ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun pruneOldLogs(keepCount: Int = 3000)

    @Query("DELETE FROM hook_logs")
    suspend fun clearLogs()
}
