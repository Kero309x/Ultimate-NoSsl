package com.ultimate.nossl.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ultimate.nossl.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("ultimate_nossl_prefs", Context.MODE_PRIVATE)
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Export Settings
        SettingsCard(
            icon = Icons.Default.FileDownload,
            title = "Export Settings",
            description = "Copy all hook & app settings to clipboard as JSON",
            iconTint = Color(0xFF10B981),
            onClick = {
                val allPrefs = prefs.all
                val json = org.json.JSONObject()
                allPrefs.forEach { (key, value) ->
                    when (value) {
                        is Boolean -> json.put(key, value)
                        is String -> json.put(key, value)
                        is Int -> json.put(key, value)
                        is Float -> json.put(key, value)
                        is Long -> json.put(key, value)
                    }
                }
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("UltimateNoSSL_Settings", json.toString(2)))
                Toast.makeText(context, "Settings exported to clipboard", Toast.LENGTH_SHORT).show()
            }
        )

        // Import Settings
        SettingsCard(
            icon = Icons.Default.FileUpload,
            title = "Import Settings",
            description = "Paste settings from clipboard and apply",
            iconTint = Color(0xFF3B82F6),
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = clipboard.primaryClip
                val text = clipData?.getItemAt(0)?.text?.toString()
                if (text != null) {
                    try {
                        val json = org.json.JSONObject(text)
                        val editor = prefs.edit()
                        json.keys().forEach { key ->
                            when (val value = json.get(key)) {
                                is Boolean -> editor.putBoolean(key, value)
                                is String -> editor.putString(key, value)
                                is Int -> editor.putInt(key, value)
                                is Double -> editor.putFloat(key, value.toFloat())
                                is Long -> editor.putLong(key, value)
                            }
                        }
                        editor.apply()
                        Toast.makeText(context, "Settings imported successfully. Restart module to apply.", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Invalid settings format", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                }
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Clear Logs
        SettingsCard(
            icon = Icons.Default.DeleteSweep,
            title = "Clear All Logs",
            description = "Remove all hook activity logs from database",
            iconTint = Color(0xFFF59E0B),
            onClick = { showClearDialog = true }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // About
        SettingsCard(
            icon = Icons.Default.Info,
            title = "About Ultimate NoSSL",
            description = "Version 2.0 — Universal SSL Pinning Bypass Engine for Android",
            iconTint = Color(0xFF8B5CF6),
            onClick = { }
        )

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Logs?") },
            text = { Text("This action cannot be undone. All hook activity logs will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearLogs()
                        showClearDialog = false
                        Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    description: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}