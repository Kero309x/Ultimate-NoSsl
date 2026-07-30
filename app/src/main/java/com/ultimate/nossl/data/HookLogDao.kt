
package com.ultimate.nossl.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HookLogDao {
    @Query("SELECT * FROM hook_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<HookLogEntity>>

    @Insert
    suspend fun insertLog(log: HookLogEntity)

    @Query("DELETE FROM hook_logs")
    suspend fun clearLogs()
}
