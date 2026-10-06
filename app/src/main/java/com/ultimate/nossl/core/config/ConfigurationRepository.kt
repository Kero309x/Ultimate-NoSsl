package com.ultimate.nossl.core.config

interface ConfigurationRepository {
    fun isHookEnabled(hookId: String): Boolean
    fun isTargetAppEnabled(packageName: String): Boolean
    fun setHookEnabled(hookId: String, enabled: Boolean)
    fun setTargetAppEnabled(packageName: String, enabled: Boolean)
    fun getConfiguration(): NoSslConfiguration
}
