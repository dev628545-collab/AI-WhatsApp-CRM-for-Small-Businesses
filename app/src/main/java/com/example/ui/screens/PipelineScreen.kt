package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.LeadEntity
import com.example.viewmodel.CrmViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipelineScreen(
    viewModel: CrmViewModel,
    onLeadSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val leads by viewModel.leads.collectAsState()
    
    // Compute stats dynamically
    val totalPipelineValue = leads.filter { it.status != "ARCHIVED" && it.pipelineStage != "CLOSED LOST" }.sumOf { it.dealValue }
    val hotLeadsCount = leads.count { it.status == "HOT" }
    val newLeadsCount = leads.count { it.status == "NEW" }
    val followUpCount = leads.count { it.status == "FOLLOW-UP" }

    var showAddLeadDialog by remember { mutableStateOf(false) }

    // Standard Pipeline Stages
    val stages = listOf("NEW LEAD", "CONTACTED", "QUOTE SENT", "CLOSED WON")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Sales Pipeline",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                },
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search pipeline")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC)) // Slate background
                .padding(innerPadding)
        ) {
            // Pipeline Summary Header Card
            PipelineSummaryCard(
                totalValue = totalPipelineValue,
                hotCount = hotLeadsCount,
                newCount = newLeadsCount,
                followUpCount = followUpCount
            )

            // Horizontal Kanban Board
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                stages.forEach { stage ->
                    val stageLeads = leads.filter { it.pipelineStage == stage }
                    PipelineColumn(
                        stageTitle = stage,
                        leads = stageLeads,
                        onLeadClick = onLeadSelected,
                        onAddLeadClick = { showAddLeadDialog = true },
                        onMoveLead = { leadId, nextStage ->
                            viewModel.updatePipelineStage(leadId, nextStage)
                        }
                    )
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
fun PipelineSummaryCard(
    totalValue: Double,
    hotCount: Int,
    newCount: Int,
    followUpCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("pipeline_summary_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total Pipeline Value",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                val formattedVal = NumberFormat.getCurrencyInstance(Locale.US).apply {
                    maximumFractionDigits = 0
                }.format(totalValue)
                Text(
                    text = formattedVal,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

            // High-Contrast Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hot Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEF4444))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Hot $hotCount",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // New Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF3B82F6))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "New $newCount",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PipelineColumn(
    stageTitle: String,
    leads: List<LeadEntity>,
    onLeadClick: (Int) -> Unit,
    onAddLeadClick: () -> Unit,
    onMoveLead: (Int, String) -> Unit
) {
    val stageValue = leads.sumOf { it.dealValue }
    val formattedValue = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }.format(stageValue)

    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
    ) {
        // Stage Title Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$stageTitle (${leads.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
            }
            Text(
                text = formattedValue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(leads, key = { it.id }) { lead ->
                PipelineLeadCard(
                    lead = lead,
                    onClick = { onLeadClick(lead.id) },
                    onMoveForward = {
                        val nextStage = getNextStage(stageTitle)
                        if (nextStage != null) {
                            onMoveLead(lead.id, nextStage)
                        }
                    },
                    onMoveBackward = {
                        val prevStage = getPrevStage(stageTitle)
                        if (prevStage != null) {
                            onMoveLead(lead.id, prevStage)
                        }
                    }
                )
            }

            item {
                // Add Lead dotted card container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = 1.dp,
                            color = Color.LightGray,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(Color.White)
                        .clickable { onAddLeadClick() }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Lead",
                            tint = Color(0xFF00C853),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Lead",
                            color = Color(0xFF00C853),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PipelineLeadCard(
    lead: LeadEntity,
    onClick: () -> Unit,
    onMoveForward: () -> Unit,
    onMoveBackward: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("pipeline_lead_card_${lead.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lead.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                // More Options Menu for transitioning
                Box {
                    IconButton(
                        onClick = { expandedMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Move Stage",
                            tint = Color(0xFF94A3B8)
                        )
                    }

                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Move Forward ➡️") },
                            onClick = {
                                onMoveForward()
                                expandedMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move Backward ⬅️") },
                            onClick = {
                                onMoveBackward()
                                expandedMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val formattedValue = NumberFormat.getCurrencyInstance(Locale.US).apply {
                maximumFractionDigits = 0
            }.format(lead.dealValue)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedValue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF00C853) // Green for cash value
                )

                if (lead.status == "HOT") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFE4E6))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "HOT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE11D48)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Time subtext and quick WhatsApp action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Time",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = lead.lastMessageTime,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // WhatsApp green button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF00C853))
                        .clickable { onClick() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = "WhatsApp Chat",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "WhatsApp",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun getNextStage(current: String): String? {
    return when (current) {
        "NEW LEAD" -> "CONTACTED"
        "CONTACTED" -> "QUOTE SENT"
        "QUOTE SENT" -> "CLOSED WON"
        else -> null
    }
}

private fun getPrevStage(current: String): String? {
    return when (current) {
        "CLOSED WON" -> "QUOTE SENT"
        "QUOTE SENT" -> "CONTACTED"
        "CONTACTED" -> "NEW LEAD"
        else -> null
    }
}
