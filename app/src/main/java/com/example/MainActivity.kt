package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CrmViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: CrmViewModel by viewModels()

    enum class ScreenTab { INBOX, PIPELINE, DASHBOARD, SETTINGS }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableStateOf(ScreenTab.INBOX) }
                val selectedLeadId by viewModel.selectedLeadId.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color.White,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == ScreenTab.INBOX && selectedLeadId == null,
                                onClick = {
                                    viewModel.selectLead(null)
                                    currentTab = ScreenTab.INBOX
                                },
                                icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Inbox", modifier = Modifier.size(24.dp)) },
                                label = { Text("Inbox") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00C853),
                                    selectedTextColor = Color(0xFF00C853),
                                    indicatorColor = Color(0xFFDCFCE7),
                                    unselectedIconColor = Color(0xFF64748B),
                                    unselectedTextColor = Color(0xFF64748B)
                                ),
                                modifier = Modifier.testTag("nav_inbox")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.PIPELINE && selectedLeadId == null,
                                onClick = {
                                    viewModel.selectLead(null)
                                    currentTab = ScreenTab.PIPELINE
                                },
                                icon = { Icon(Icons.Default.ViewWeek, contentDescription = "Pipeline", modifier = Modifier.size(24.dp)) },
                                label = { Text("Pipeline") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00C853),
                                    selectedTextColor = Color(0xFF00C853),
                                    indicatorColor = Color(0xFFDCFCE7),
                                    unselectedIconColor = Color(0xFF64748B),
                                    unselectedTextColor = Color(0xFF64748B)
                                ),
                                modifier = Modifier.testTag("nav_pipeline")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.DASHBOARD && selectedLeadId == null,
                                onClick = {
                                    viewModel.selectLead(null)
                                    currentTab = ScreenTab.DASHBOARD
                                },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard", modifier = Modifier.size(24.dp)) },
                                label = { Text("Dashboard") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00C853),
                                    selectedTextColor = Color(0xFF00C853),
                                    indicatorColor = Color(0xFFDCFCE7),
                                    unselectedIconColor = Color(0xFF64748B),
                                    unselectedTextColor = Color(0xFF64748B)
                                ),
                                modifier = Modifier.testTag("nav_dashboard")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.SETTINGS && selectedLeadId == null,
                                onClick = {
                                    viewModel.selectLead(null)
                                    currentTab = ScreenTab.SETTINGS
                                },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(24.dp)) },
                                label = { Text("Settings") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00C853),
                                    selectedTextColor = Color(0xFF00C853),
                                    indicatorColor = Color(0xFFDCFCE7),
                                    unselectedIconColor = Color(0xFF64748B),
                                    unselectedTextColor = Color(0xFF64748B)
                                ),
                                modifier = Modifier.testTag("nav_settings")
                            )
                        }
                    }
                ) { innerPadding ->
                    val modifierWithPadding = Modifier.padding(innerPadding)

                    if (selectedLeadId != null) {
                        LeadDetailScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.selectLead(null) },
                            modifier = modifierWithPadding
                        )
                    } else {
                        when (currentTab) {
                            ScreenTab.INBOX -> InboxScreen(
                                viewModel = viewModel,
                                onLeadSelected = { id -> viewModel.selectLead(id) },
                                modifier = modifierWithPadding
                            )
                            ScreenTab.PIPELINE -> PipelineScreen(
                                viewModel = viewModel,
                                onLeadSelected = { id -> viewModel.selectLead(id) },
                                modifier = modifierWithPadding
                            )
                            ScreenTab.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                onLeadSelected = { id -> viewModel.selectLead(id) },
                                modifier = modifierWithPadding
                            )
                            ScreenTab.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                modifier = modifierWithPadding
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

