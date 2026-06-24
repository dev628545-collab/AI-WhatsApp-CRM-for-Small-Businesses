package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.ChatMessageEntity
import com.example.data.database.LeadEntity
import com.example.viewmodel.CrmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadDetailScreen(
    viewModel: CrmViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lead by viewModel.selectedLead.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()
    val suggestion by viewModel.activeAiSuggestion.collectAsState()
    val isGeneratingAi by viewModel.isGeneratingAi.collectAsState()

    var customMessageText by remember { mutableStateOf("") }
    var editingSuggestionText by remember { mutableStateOf("") }
    var showEditSuggestionDialog by remember { mutableStateOf(false) }
    var showQuoteDialog by remember { mutableStateOf(false) }

    val currentLead = lead ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentLead.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            StatusBadge(status = currentLead.status)
                        }
                        Text(
                            text = "${currentLead.category} • ${currentLead.lastMessageTime}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.triggerAiSuggestion(currentLead.id)
                        Toast.makeText(context, "Regenerating AI Suggestion...", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Regenerate AI", tint = Color(0xFF00C853))
                    }
                    IconButton(onClick = { /* Search chat */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search chat")
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
                .background(Color(0xFFF1F5F9)) // Soft slate light bg for chat
                .padding(innerPadding)
        ) {
            // Action buttons row (Call, Quote, Archive)
            ActionButtonsRow(
                lead = currentLead,
                onCall = {
                    Toast.makeText(context, "Initiating Call to ${currentLead.name} (${currentLead.phone})...", Toast.LENGTH_LONG).show()
                },
                onQuote = { showQuoteDialog = true },
                onArchive = {
                    viewModel.updateLeadStatus(currentLead.id, "ARCHIVED")
                    Toast.makeText(context, "Lead archived.", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            )

            // Scrollable Content area (Messages + AI Suggestion + Lead details)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Messages List
                item {
                    Text(
                        text = "TUESDAY, OCT 24", // Chronological separator
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                items(messages, key = { it.id }) { message ->
                    ChatBubble(message = message)
                }

                // AI Suggestion Box
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    AiSuggestionCard(
                        suggestionText = suggestion?.suggestionText,
                        matchPercent = suggestion?.matchPercentage ?: 95,
                        isLoading = isGeneratingAi,
                        onSend = {
                            suggestion?.suggestionText?.let { txt ->
                                viewModel.sendMessage(currentLead.id, txt, isFromClient = false)
                                Toast.makeText(context, "Response sent via WhatsApp!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onEdit = {
                            editingSuggestionText = suggestion?.suggestionText ?: ""
                            showEditSuggestionDialog = true
                        }
                    )
                }

                // Lead Details Section
                item {
                    LeadDetailsCard(
                        lead = currentLead,
                        onViewLocation = {
                            Toast.makeText(context, "Opening Map View for Landscaping Redesign...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Custom Message Composer Bar (fully interactive!)
            MessageComposer(
                text = customMessageText,
                onTextChange = { customMessageText = it },
                onSend = {
                    if (customMessageText.trim().isNotEmpty()) {
                        viewModel.sendMessage(currentLead.id, customMessageText.trim(), isFromClient = false)
                        customMessageText = ""
                    }
                },
                onReceiveSimulate = {
                    // Quick simulated reply to test the CRM flow
                    val clientSimulates = listOf(
                        "How much do you charge for delivery?",
                        "Can you send over the portfolio of backyard garden designs?",
                        "I am ready to proceed. What is the contract deposit?",
                        "Do you offer any discount for first-time orders?"
                    )
                    viewModel.sendMessage(currentLead.id, clientSimulates.random(), isFromClient = true)
                    Toast.makeText(context, "Client simulated message sent!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // Dialog for Editing AI Suggestion
    if (showEditSuggestionDialog) {
        AlertDialog(
            onDismissRequest = { showEditSuggestionDialog = false },
            title = { Text("Edit Suggested Reply", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editingSuggestionText,
                    onValueChange = { editingSuggestionText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("edit_suggestion_field"),
                    label = { Text("Response Text") },
                    maxLines = 10
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editingSuggestionText.isNotEmpty()) {
                            viewModel.sendMessage(currentLead.id, editingSuggestionText, isFromClient = false)
                            showEditSuggestionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) {
                    Text("Send via WhatsApp")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditSuggestionDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Dialog for Creating Custom Quote
    if (showQuoteDialog) {
        var quoteTitle by remember { mutableStateOf("Landscaping Site consultation Quote") }
        var quoteAmount by remember { mutableStateOf("850") }
        var quoteDetails by remember { mutableStateOf("Consultation, waste disposal, & drafting of formal blueprint plans.") }

        AlertDialog(
            onDismissRequest = { showQuoteDialog = false },
            title = { Text("Generate Digital Quote", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = quoteTitle,
                        onValueChange = { quoteTitle = it },
                        label = { Text("Proposal Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = quoteAmount,
                        onValueChange = { quoteAmount = it },
                        label = { Text("Amount ($)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = quoteDetails,
                        onValueChange = { quoteDetails = it },
                        label = { Text("Details & Scope") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = quoteAmount.toDoubleOrNull() ?: 0.0
                        viewModel.updatePipelineStage(currentLead.id, "QUOTE SENT")
                        viewModel.sendMessage(
                            currentLead.id,
                            "📝 *DIGITAL PROPOSAL GENERATED*\n\n*Title:* $quoteTitle\n*Value:* $$amount\n*Scope:* $quoteDetails\n\n_Please accept to finalize schedule._",
                            isFromClient = false
                        )
                        showQuoteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) {
                    Text("Generate & Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuoteDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun ActionButtonsRow(
    lead: LeadEntity,
    onCall: () -> Unit,
    onQuote: () -> Unit,
    onArchive: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 10.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = onCall,
            modifier = Modifier
                .weight(1f)
                .padding(end = 6.dp)
                .height(44.dp)
                .testTag("action_call"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF334155)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Call", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onQuote,
            modifier = Modifier
                .weight(1.1f)
                .padding(horizontal = 4.dp)
                .height(44.dp)
                .testTag("action_quote"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF334155)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Description, contentDescription = "Quote", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Quote", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onArchive,
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp)
                .height(44.dp)
                .testTag("action_archive"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFEF4444)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Archive, contentDescription = "Archive", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Archive", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessageEntity) {
    val isClient = message.isFromClient
    val bubbleBg = if (isClient) Color.White else Color(0xFF00C853)
    val textStyle = if (isClient) Color(0xFF1E293B) else Color.White
    val metaStyle = if (isClient) Color(0xFF94A3B8) else Color(0xCCE2E8F0)
    val alignment = if (isClient) Alignment.Start else Alignment.End

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isClient) 2.dp else 16.dp,
                        bottomEnd = if (isClient) 16.dp else 2.dp
                    )
                )
                .background(bubbleBg)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 290.dp)
        ) {
            Column {
                Text(
                    text = message.messageText,
                    fontSize = 15.sp,
                    color = textStyle,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = message.timestamp,
                        fontSize = 10.sp,
                        color = metaStyle
                    )
                    if (!isClient) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read status",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiSuggestionCard(
    suggestionText: String?,
    matchPercent: Int,
    isLoading: Boolean,
    onSend: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_suggestion_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFF00C853))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Sparkle",
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI SUGGESTION",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF00C853)
                    )
                }

                // Match Percentage Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0F2FE))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$matchPercent% Match",
                        color = Color(0xFF0369A1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFF00C853), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Drafting premium response...", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = suggestionText ?: "No suggestion loaded. Type/Simulate a message or tap Regenerate above.",
                        fontSize = 14.sp,
                        color = Color(0xFF334155),
                        fontStyle = FontStyle.Italic,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (suggestionText != null) {
                    Button(
                        onClick = onSend,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("send_via_whatsapp_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send via WhatsApp", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("edit_suggestion_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B)),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Response", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LeadDetailsCard(
    lead: LeadEntity,
    onViewLocation: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lead_details_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Lead Details",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lead Score progress line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Lead Score", fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                Text("${lead.leadScore}/100", fontSize = 14.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            val barColor = when {
                lead.leadScore >= 80 -> Color(0xFF00C853)
                lead.leadScore >= 60 -> Color(0xFFF97316)
                else -> Color(0xFFEF4444)
            }
            LinearProgressIndicator(
                progress = lead.leadScore / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = barColor,
                trackColor = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("CHANNEL", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ChatBubble, contentDescription = "Chat", tint = Color(0xFF00C853), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("ASSIGNED TO", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(lead.assignedTo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Map Placeholder layout (Screenshot 3 shows an aerial view layout)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                // Drawing an elegant abstract schematic representation of a landscape yard
                Image(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Map Location Preview",
                    contentScale = ContentScale.Inside,
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    alpha = 0.3f
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x33000000))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onViewLocation,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("view_location_btn")
                    ) {
                        Text("View Location", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageComposer(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onReceiveSimulate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Quick simulate incoming button
        IconButton(
            onClick = onReceiveSimulate,
            modifier = Modifier.testTag("simulate_incoming_btn")
        ) {
            Icon(Icons.Default.HelpOutline, contentDescription = "Simulate client message", tint = Color(0xFF00C853))
        }

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Write custom reply...") },
            modifier = Modifier
                .weight(1f)
                .heightIn(max = 100.dp)
                .testTag("custom_reply_input"),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00C853),
                unfocusedBorderColor = Color(0xFFCBD5E1)
            ),
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSend,
            enabled = text.trim().isNotEmpty(),
            modifier = Modifier
                .clip(CircleShape)
                .background(if (text.trim().isNotEmpty()) Color(0xFF00C853) else Color(0xFFF1F5F9))
                .testTag("custom_reply_send_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = if (text.trim().isNotEmpty()) Color.White else Color.LightGray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
