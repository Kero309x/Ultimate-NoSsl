package com.ultimate.nossl.core.config

data class NoSslConfiguration(
    val enabledHooks: Set<String>,
    val targetPackages: Set<String>
)
