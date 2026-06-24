package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.LeadEntity
import com.example.viewmodel.CrmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: CrmViewModel,
    onLeadSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredLeads by viewModel.filteredLeads.collectAsState()
    val currentFilter by viewModel.inboxFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddLeadDialog by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchExpanded) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search leads, phone, tags...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00C853),
                                unfocusedBorderColor = Color.LightGray
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("search_field"),
                            trailingIcon = {
                                IconButton(onClick = {
                                    viewModel.setSearchQuery("")
                                    isSearchExpanded = false
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close search")
                                }
                            }
                        )
                    } else {
                        Text(
                            text = "FollowUp AI",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00C853) // WhatsApp style Green
                        )
                    }
                },
                actions = {
                    if (!isSearchExpanded) {
                        IconButton(
                            onClick = { isSearchExpanded = true },
                            modifier = Modifier.testTag("search_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search leads")
                        }
                        IconButton(onClick = { /* Menu */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddLeadDialog = true },
                containerColor = Color(0xFF00C853),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp)
                    .testTag("add_lead_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Lead", modifier = Modifier.size(28.dp))
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC)) // Soft slate background
                .padding(innerPadding)
        ) {
            // Horizontal Filter Chips
            FilterChipsRow(
                currentFilter = currentFilter,
                onFilterSelected = { viewModel.setInboxFilter(it) }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Pin an AI Suggestion box if there's Sarah Jenkins in the hot list
                val hasSarah = filteredLeads.any { it.name.contains("Sarah", ignoreCase = true) }
                if (currentFilter == CrmViewModel.FilterType.ALL || currentFilter == CrmViewModel.FilterType.HOT) {
                    if (hasSarah) {
                        item {
                            AiSuggestionBanner(
                                onActionClicked = {
                                    val sarahLead = filteredLeads.firstOrNull { it.name.contains("Sarah", ignoreCase = true) }
                                    if (sarahLead != null) {
                                        onLeadSelected(sarahLead.id)
                                    }
                                }
                            )
                        }
                    }
                }

                if (filteredLeads.isEmpty()) {
                    item {
                        EmptyStateView(query = searchQuery, filter = currentFilter)
                    }
                } else {
                    items(filteredLeads, key = { it.id }) { lead ->
                        LeadItemRow(
                            lead = lead,
                            onClick = { onLeadSelected(lead.id) }
                        )
                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                    }
                }
            }
        }
    }

    if (showAddLeadDialog) {
        AddLeadDialog(
            onDismiss = { showAddLeadDialog = false },
            onConfirm = { name, phone, category, value, message ->
                viewModel.addNewLead(name, phone, category, value, "NEW", message)
                showAddLeadDialog = false
            }
        )
    }
}

@Composable
fun FilterChipsRow(
    currentFilter: CrmViewModel.FilterType,
    onFilterSelected: (CrmViewModel.FilterType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CrmViewModel.FilterType.values().forEach { filterType ->
            val label = when (filterType) {
                CrmViewModel.FilterType.ALL -> "All"
                CrmViewModel.FilterType.UNREAD -> "Unread"
                CrmViewModel.FilterType.HOT -> "🔥 Hot"
                CrmViewModel.FilterType.ARCHIVED -> "Archived"
            }
            val isSelected = currentFilter == filterType
            val chipBg = if (isSelected) Color(0xFF00C853) else Color(0xFFF1F5F9)
            val chipText = if (isSelected) Color.White else Color(0xFF64748B)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(chipBg)
                    .clickable { onFilterSelected(filterType) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("filter_chip_${filterType.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = chipText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun AiSuggestionBanner(
    onActionClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Green vertical line on the left to denote "AI feature"
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(170.dp)
                    .background(Color(0xFF00C853))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Sparkle",
                            tint = Color(0xFF00C853),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Suggestion",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Text(
                        text = "Just now",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Follow up with Sarah. She expressed interest in the premium package 2 hours ago but hasn't booked yet.",
                    fontSize = 14.sp,
                    color = Color(0xFF475569),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onActionClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("send_suggested_reply_banner_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = "Chat",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Suggested Reply", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LeadItemRow(
    lead: LeadEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Beautiful circular Avatar with initials
        val avatarBg = when (lead.status) {
            "HOT" -> Color(0xFFFEF2F2)
            "NEW" -> Color(0xFFF0FDF4)
            "FOLLOW-UP" -> Color(0xFFFFF7ED)
            else -> Color(0xFFF1F5F9)
        }
        val avatarText = when (lead.status) {
            "HOT" -> Color(0xFFEF4444)
            "NEW" -> Color(0xFF22C55E)
            "FOLLOW-UP" -> Color(0xFFF97316)
            else -> Color(0xFF64748B)
        }

        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = lead.assignedToInitials,
                color = avatarText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            // Green "Unread" dot badge on avatar
            if (lead.unread) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00C853))
                        .align(Alignment.TopEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lead.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = lead.lastMessageTime,
                    fontSize = 12.sp,
                    color = if (lead.unread) Color(0xFF00C853) else Color(0xFF94A3B8),
                    fontWeight = if (lead.unread) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = lead.lastMessage,
                fontSize = 14.sp,
                color = if (lead.unread) Color(0xFF000000) else Color(0xFF64748B),
                fontWeight = if (lead.unread) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier.padding(end = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Lead Status Chip
                    StatusBadge(status = lead.status)
                    // Reply tag or sub-tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = lead.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                    }
                }

                Text(
                    text = "Assigned: ${lead.assignedToInitials}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, text) = when (status) {
        "HOT" -> Color(0xFFFFE4E6) to Color(0xFFE11D48)
        "NEW" -> Color(0xFFDCFCE7) to Color(0xFF16A34A)
        "FOLLOW-UP" -> Color(0xFFFEF3C7) to Color(0xFFD97706)
        "REPLIED" -> Color(0xFFE0F2FE) to Color(0xFF0284C7)
        else -> Color(0xFFE2E8F0) to Color(0xFF475569)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = status,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = text
        )
    }
}

@Composable
fun EmptyStateView(query: String, filter: CrmViewModel.FilterType) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = "Empty",
            tint = Color.LightGray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (query.isNotEmpty()) "No results found" else "No leads in this view",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF475569)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (query.isNotEmpty()) "Try searching for a different name, category, or tag." else "Leads in the category '${filter.name}' will appear here.",
            fontSize = 14.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLeadDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Landscaping") }
    var valueStr by remember { mutableStateOf("") }
    var initialMessage by remember { mutableStateOf("") }

    val categories = listOf("Landscaping", "Web Dev", "Branding", "App Build", "Consulting")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import / Add New Lead", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Lead Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_lead_name")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("WhatsApp Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_lead_phone")
                )
                
                // Simple select category text (or dropdown simulation)
                Text("Select Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = category == cat
                        val bg = if (isSelected) Color(0xFF00C853) else Color(0xFFF1F5F9)
                        val txt = if (isSelected) Color.White else Color(0xFF475569)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .clickable { category = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(cat, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = txt)
                        }
                    }
                }

                OutlinedTextField(
                    value = valueStr,
                    onValueChange = { valueStr = it },
                    label = { Text("Estimated Deal Value ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_lead_value")
                )
                OutlinedTextField(
                    value = initialMessage,
                    onValueChange = { initialMessage = it },
                    label = { Text("First Client Message (triggers AI)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("add_lead_msg")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && phone.isNotEmpty()) {
                        val dealVal = valueStr.toDoubleOrNull() ?: 1000.0
                        onConfirm(name, phone, category, dealVal, initialMessage)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
            ) {
                Text("Import Lead")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
