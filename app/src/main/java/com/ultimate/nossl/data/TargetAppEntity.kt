
package com.ultimate.nossl.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "target_apps")
data class TargetAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val isSystemApp: Boolean = false,
    val lastHookTime: Long = 0,
    val notes: String = ""
)
