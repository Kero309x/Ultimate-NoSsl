
package com.ultimate.nossl.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TargetAppDao {
    @Query("SELECT * FROM target_apps ORDER BY appName ASC")
    fun getAllTargets(): Flow<List<TargetAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(target: TargetAppEntity)

    @Query("DELETE FROM target_apps WHERE packageName = :packageName")
    suspend fun deleteTarget(packageName: String)
}
