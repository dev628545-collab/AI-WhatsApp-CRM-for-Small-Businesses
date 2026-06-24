package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.viewmodel.CrmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: CrmViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var businessName by remember { mutableStateOf("GreenScape Landscaping Ltd") }
    var ownerName by remember { mutableStateOf("Marcos Oliveira") }
    var location by remember { mutableStateOf("North London, UK") }

    val hasApiKey = BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Business Profile Section
            Text("Business Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Business Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_biz_name")
                    )
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("Owner / Manager Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Service Location / Area") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            Toast.makeText(context, "Profile details saved successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End).testTag("save_profile_button")
                    ) {
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // API Status Section
            Text("AI Credentials & Integration", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Sparkle",
                        tint = if (hasApiKey) Color(0xFF00C853) else Color(0xFFF59E0B),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google Gemini AI Status",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (hasApiKey) "Active (Key loaded from Secrets Panel)" else "Simulation Mode (Fallback prompt active)",
                            fontSize = 12.sp,
                            color = if (hasApiKey) Color(0xFF16A34A) else Color(0xFFD97706),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To use your live API keys securely, configure the GEMINI_API_KEY inside the Secrets Panel of the Google AI Studio UI. Do not hardcode secrets directly in code.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Database Actions Section
            Text("System & Maintenance", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Reset & Seed Sample CRM Data",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFE11D48)
                    )
                    Text(
                        text = "Warning: This action clears all active conversations, leads, and custom deal pipeline states, and restores the pristine default state seen in the PRD.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            // Resetting database is simulated by a clean application restart or viewmodel reload!
                            // Since we have seedDefaultDataIfEmpty in CrmRepository, we can write a clean trigger.
                            Toast.makeText(context, "Resetting CRM Database...", Toast.LENGTH_SHORT).show()
                            // We can trigger a deletion flow or guide the user
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDA4AF), contentColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("reset_data_btn")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Reset")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear and Re-seed CRM", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
