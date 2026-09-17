
package com.ultimate.nossl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.ultimate.nossl.ui.MainViewModel
import com.ultimate.nossl.ui.screens.DashboardScreen
import com.ultimate.nossl.ui.screens.HooksScreen
import com.ultimate.nossl.ui.screens.LogScreen
import com.ultimate.nossl.ui.screens.SettingsScreen
import com.ultimate.nossl.ui.screens.TargetAppsScreen
import com.ultimate.nossl.ui.theme.UltimateNoSSLTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            UltimateNoSSLTheme {
                MainAppScreen(viewModel)
            }
        }
    }
}

enum class Screen(val label: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Default.Home),
    Hooks("Hooks", Icons.Default.Build),
    Targets("Targets", Icons.Default.Apps),
    Logs("Logs", Icons.Default.List),
    Settings("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(Screen.Dashboard) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = selectedTab == screen,
                        onClick = { selectedTab = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                Screen.Dashboard -> DashboardScreen(viewModel)
                Screen.Hooks -> HooksScreen(viewModel)
                Screen.Targets -> TargetAppsScreen(viewModel)
                Screen.Logs -> LogScreen(viewModel)
                Screen.Settings -> SettingsScreen(viewModel)
            }
        }
    }
}
