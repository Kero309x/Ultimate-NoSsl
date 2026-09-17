package com.ultimate.nossl.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultimate.nossl.R
import com.ultimate.nossl.ui.MainViewModel

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val isXposedActive by viewModel.isXposedActive.collectAsState()
    val enabledHookCount by viewModel.enabledHookCount.collectAsState()
    val logCount by viewModel.logCount.collectAsState()
    val targetCount by viewModel.targetCount.collectAsState()
    val context = LocalContext.current

    val activeEmerald = Color(0xFF10B981)
    val inactiveCrimson = Color(0xFFEF4444)
    val darkBg = Color(0xFF0F172A)

    val statusColor by animateColorAsState(
        targetValue = if (isXposedActive) activeEmerald else inactiveCrimson,
        animationSpec = tween(400),
        label = "statusColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // App Icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(130.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = darkBg,
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(3.dp, statusColor),
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "App Icon",
                        modifier = Modifier.size(90.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 4.dp, end = 4.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(statusColor)
                    .border(3.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isXposedActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Status Badge
        Surface(
            shape = CircleShape,
            color = statusColor.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isXposedActive) "ACTIVE" else "INACTIVE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    letterSpacing = 1.2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title - single line
        Text(
            text = if (isXposedActive) "SSL Bypass Engaged" else "Module Disconnected",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isXposedActive)
                "Intercepting network calls system-wide."
            else
                "Enable module in your Xposed manager and reboot.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(title = "Hooks", value = "$enabledHookCount", subtitle = "active", color = Color(0xFF6366F1), modifier = Modifier.weight(1f))
            StatCard(title = "Targets", value = "$targetCount", subtitle = "apps", color = Color(0xFF8B5CF6), modifier = Modifier.weight(1f))
            StatCard(title = "Logs", value = "$logCount", subtitle = "entries", color = Color(0xFF06B6D4), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Credits
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Developer Credits",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0088CC),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { openTelegramNative(context, "its_kero309x") }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(painter = painterResource(id = R.drawable.ic_telegram), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Telegram", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                Text("@its_kero309x", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF24292E),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { openGitHubNative(context, "kero309x") }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(painter = painterResource(id = R.drawable.ic_github), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("GitHub", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                Text("kero309x", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StatCard(title: String, value: String, subtitle: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

private fun openTelegramNative(context: Context, username: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=$username")).apply { setPackage("org.telegram.messenger") })
    } catch (_: Exception) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$username"))) } catch (_: Exception) {}
    }
}

private fun openGitHubNative(context: Context, username: String) {
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/$username"))) } catch (_: Exception) {}
}
