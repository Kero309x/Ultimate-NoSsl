
package com.ultimate.nossl.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hook_logs",
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["level"]),
        Index(value = ["timestamp"])
    ]
)
data class HookLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val packageName: String,
    val message: String,
    val level: String = "INFO" // INFO, WARN, ERROR, HOOK
)
