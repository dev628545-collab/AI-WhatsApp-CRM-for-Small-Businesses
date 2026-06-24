package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
fun DashboardScreen(
    viewModel: CrmViewModel,
    onLeadSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val leads by viewModel.leads.collectAsState()

    // Filter Hot leads specifically for the summary list
    val hotLeads = leads.filter { it.status == "HOT" || it.status == "REPLIED" }.take(3)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FollowUp AI",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00C853)
                    )
                },
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search Dashboard")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stat 1: Lead Conversion MTD
            item {
                MetricCard(
                    title = "LEAD CONVERSION",
                    value = "24.8%",
                    subtext = "4.2%",
                    showTrend = true,
                    indicator = {
                        Box(
                            modifier = Modifier.size(54.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = 0.248f,
                                modifier = Modifier.fillMaxSize(),
                                color = Color(0xFF00C853),
                                trackColor = Color(0xFFE2E8F0),
                                strokeWidth = 5.dp
                            )
                            Icon(Icons.Default.TrendingUp, contentDescription = "Trend", tint = Color(0xFF00C853), modifier = Modifier.size(20.dp))
                        }
                    }
                )
            }

            // Stat 2: Total Revenue MTD
            item {
                MetricCard(
                    title = "TOTAL REVENUE (MTD)",
                    value = "$42,500",
                    subtext = "Goal: $50,000",
                    showTrend = false,
                    indicator = {
                        // Drawing a mini bar chart layout with pure Compose elements
                        Row(
                            modifier = Modifier
                                .height(54.dp)
                                .width(64.dp)
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val barHeights = listOf(0.4f, 0.6f, 1f, 0.7f)
                            barHeights.forEach { h ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(h)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(Color(0xFF00C853))
                                )
                            }
                        }
                    }
                )
            }

            // Team Performance List
            item {
                TeamPerformanceSection()
            }

            // Hot Leads Summary Section
            item {
                Text(
                    text = "Hot Leads Summary",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (hotLeads.isEmpty()) {
                item {
                    Text("No hot leads in the system currently.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                items(hotLeads, key = { it.id }) { lead ->
                    HotLeadSummaryRow(
                        lead = lead,
                        onClick = { onLeadSelected(lead.id) }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtext: String,
    showTrend: Boolean,
    indicator: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = value,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showTrend) {
                        Text(
                            text = "↗ $subtext",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00C853)
                        )
                    } else {
                        Text(
                            text = subtext,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            indicator()
        }
    }
}

@Composable
fun TeamPerformanceSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    text = "Team Performance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "View All ›",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00C853)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val team = listOf(
                Triple("David Miller", "Senior Closer", "12 Leads Closed"),
                Triple("Sarah Chen", "Lead Specialist", "9 Leads Closed"),
                Triple("Marcus Wright", "Account Exec", "7 Leads Closed")
            )

            team.forEachIndexed { index, (name, role, closed) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circle Avatar
                    val initials = name.split(" ").map { it.first() }.joinToString("")
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initials, color = Color(0xFF475569), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        // Online status green indicator dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (index < 2) Color(0xFF00C853) else Color.LightGray)
                                .align(Alignment.BottomEnd)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E293B))
                        Text(role, fontSize = 12.sp, color = Color(0xFF64748B))
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val valueColor = if (index == 0) Color(0xFF00C853) else Color(0xFF475569)
                        Text(
                            text = closed.split(" ").first(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = valueColor
                        )
                        Text("Leads Closed", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                }
                if (index < team.size - 1) {
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
fun HotLeadSummaryRow(
    lead: LeadEntity,
    onClick: () -> Unit
) {
    val (actionTag, subtext, tagColor) = when (lead.id) {
        1 -> Triple("IMMEDIATE ACTION", "AI: Suggests Follow-up now", Color(0xFFEF4444))
        2, 7 -> Triple("REPLIED", "Follow-up: 2h ago", Color(0xFFF97316))
        else -> Triple("NEW MESSAGE", "AI: Drafted response ready", Color(0xFF22C55E))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("hot_lead_row_${lead.id}"),
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(tagColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = actionTag,
                        color = tagColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val formattedValue = NumberFormat.getCurrencyInstance(Locale.US).apply {
                maximumFractionDigits = 0
            }.format(lead.dealValue)

            Text(
                text = "Budget: $formattedValue • ${lead.category}",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // AI Action Sub-Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Action",
                    tint = Color(0xFF00C853),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = subtext,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF00C853)
                )
            }
        }
    }
}
