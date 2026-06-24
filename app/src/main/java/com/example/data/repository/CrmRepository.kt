package com.example.data.repository

import android.util.Log
import com.example.data.api.CrmGeminiService
import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first

class CrmRepository(
    private val leadDao: LeadDao,
    private val chatMessageDao: ChatMessageDao,
    private val aiSuggestionDao: AiSuggestionDao
) {
    private val TAG = "CrmRepository"

    val allLeads: Flow<List<LeadEntity>> = leadDao.getAllLeads()

    fun getLeadById(id: Int): Flow<LeadEntity?> = leadDao.getLeadByIdFlow(id)

    fun getMessagesForLead(leadId: Int): Flow<List<ChatMessageEntity>> = chatMessageDao.getMessagesForLead(leadId)

    fun getSuggestionForLead(leadId: Int): Flow<AiSuggestionEntity?> = aiSuggestionDao.getSuggestionForLead(leadId)

    suspend fun insertLead(lead: LeadEntity): Long = leadDao.insertLead(lead)

    suspend fun updateLead(lead: LeadEntity) = leadDao.updateLead(lead)

    suspend fun deleteLead(lead: LeadEntity) = leadDao.deleteLead(lead)

    suspend fun updateLeadPipelineStage(leadId: Int, stage: String) = leadDao.updateLeadPipelineStage(leadId, stage)

    suspend fun updateLeadStatus(leadId: Int, status: String) = leadDao.updateLeadStatus(leadId, status)

    suspend fun insertMessage(message: ChatMessageEntity): Long {
        val id = chatMessageDao.insertMessage(message)
        // Also update the lead's last message and last message time/timestamp
        leadDao.updateLeadLastMessage(
            leadId = message.leadId,
            message = message.messageText,
            time = message.timestamp,
            timestamp = message.timestampLong
        )
        return id
    }

    /**
     * Triggers Gemini API to generate a smart reply draft and update lead score dynamically.
     */
    suspend fun generateAiSuggestion(leadId: Int) {
        val lead = leadDao.getLeadById(leadId) ?: return
        val messages = chatMessageDao.getMessagesForLead(leadId).first()
        
        val conversationHistory = messages.map { it.isFromClient to it.messageText }
        
        Log.d(TAG, "Requesting AI suggestion for lead: ${lead.name}")
        val result = CrmGeminiService.generateSuggestionAndScore(
            leadName = lead.name,
            category = lead.category,
            conversationHistory = conversationHistory
        )

        // Delete any old suggestions
        aiSuggestionDao.deleteSuggestionForLead(leadId)

        // Insert new suggestion
        aiSuggestionDao.insertSuggestion(
            AiSuggestionEntity(
                leadId = leadId,
                suggestionText = result.suggestion,
                matchPercentage = result.matchPercentage,
                reason = result.reason
            )
        )

        // Update lead score in the main LeadEntity
        leadDao.updateLeadScore(leadId, result.leadScore)
    }

    /**
     * Seeds default sample data matching the visual specs.
     */
    suspend fun seedDefaultDataIfEmpty() {
        val existingLeads = leadDao.getAllLeads().first()
        if (existingLeads.isNotEmpty()) {
            Log.d(TAG, "Database already populated. Skipping seeding.")
            return
        }

        Log.d(TAG, "Database is empty. Seeding default sample data...")

        val now = System.currentTimeMillis()

        val sampleLeads = listOf(
            LeadEntity(
                id = 1,
                name = "Marcos Oliveira",
                phone = "+44 7911 987654",
                status = "HOT",
                assignedTo = "Me (You)",
                assignedToInitials = "ME",
                lastMessage = "Saturday at 10:00 AM sounds perfect. Can you confirm the price for the initial consultation?",
                lastMessageTime = "11:42 AM",
                lastMessageTimestamp = now - 50 * 60 * 1000, // 50 mins ago
                pipelineStage = "CONTACTED",
                dealValue = 850.0,
                leadScore = 85,
                category = "Landscaping"
            ),
            LeadEntity(
                id = 2,
                name = "Sarah Jenkins",
                phone = "+44 7911 123456",
                status = "HOT",
                assignedTo = "Sarah Jenkins",
                assignedToInitials = "SJ",
                lastMessage = "That sounds perfect! What are the next ...",
                lastMessageTime = "10:45 AM",
                lastMessageTimestamp = now - 2 * 60 * 60 * 1000, // 2h ago
                pipelineStage = "NEW LEAD",
                dealValue = 1200.0,
                leadScore = 92,
                category = "Landscaping"
            ),
            LeadEntity(
                id = 3,
                name = "Alex Rivera",
                phone = "+1 555-010-4455",
                status = "HOT",
                assignedTo = "Me (You)",
                assignedToInitials = "ME",
                lastMessage = "I would like to start immediately if possible.",
                lastMessageTime = "2 hours ago",
                lastMessageTimestamp = now - 2 * 60 * 60 * 1000 - 5 * 60 * 1000,
                pipelineStage = "NEW LEAD",
                dealValue = 850.0,
                leadScore = 90,
                category = "Web Dev"
            ),
            LeadEntity(
                id = 4,
                name = "Marcus Rivera",
                phone = "+1 555-019-2834",
                status = "NEW",
                assignedTo = "Alex Lopez",
                assignedToInitials = "AL",
                lastMessage = "Can you send over the pricing guide a...",
                lastMessageTime = "8:12 AM",
                lastMessageTimestamp = now - 4 * 60 * 60 * 1000,
                pipelineStage = "QUOTE SENT",
                dealValue = 2450.0,
                leadScore = 78,
                category = "Web Dev",
                unread = true
            ),
            LeadEntity(
                id = 5,
                name = "Elena Rodriguez",
                phone = "+34 612 345 678",
                status = "FOLLOW-UP",
                assignedTo = "Michael Edwards",
                assignedToInitials = "ME",
                lastMessage = "I'll check with my manager and get back...",
                lastMessageTime = "Yesterday",
                lastMessageTimestamp = now - 24 * 60 * 60 * 1000,
                pipelineStage = "NEW LEAD",
                dealValue = 3100.0,
                leadScore = 65,
                category = "Branding"
            ),
            LeadEntity(
                id = 6,
                name = "David Chen",
                phone = "+1 415-555-2671",
                status = "COLD",
                assignedTo = "Chloe Young",
                assignedToInitials = "CY",
                lastMessage = "Thanks for the info. We'll keep you...",
                lastMessageTime = "Jan 24",
                lastMessageTimestamp = now - 150 * 24 * 60 * 60 * 1000,
                pipelineStage = "CLOSED LOST",
                dealValue = 1500.0,
                leadScore = 35,
                category = "App Build"
            ),
            LeadEntity(
                id = 7,
                name = "Elena Gomez",
                phone = "+34 699 112 233",
                status = "REPLIED",
                assignedTo = "Sarah Chen",
                assignedToInitials = "SC",
                lastMessage = "The design files look amazing. I'll review with the team.",
                lastMessageTime = "2h ago",
                lastMessageTimestamp = now - 2 * 60 * 60 * 1000,
                pipelineStage = "CONTACTED",
                dealValue = 8500.0,
                leadScore = 88,
                category = "Branding"
            ),
            LeadEntity(
                id = 8,
                name = "Jordan Smith",
                phone = "+1 650-555-8899",
                status = "NEW",
                assignedTo = "Me (You)",
                assignedToInitials = "ME",
                lastMessage = "Drafted response ready.",
                lastMessageTime = "Just now",
                lastMessageTimestamp = now - 1 * 60 * 1000,
                pipelineStage = "NEW LEAD",
                dealValue = 22000.0,
                leadScore = 95,
                category = "App Build"
            )
        )

        leadDao.insertLeads(sampleLeads)

        // Seed Chat Messages for Marcos Oliveira (Lead ID = 1)
        val sampleMessages = listOf(
            ChatMessageEntity(
                leadId = 1,
                messageText = "Hi there! I saw your landscaping portfolio. Do you have availability to do a site visit in North London this weekend? We're looking for a complete backyard redesign.",
                timestamp = "10:14 AM",
                timestampLong = now - 90 * 60 * 1000,
                isFromClient = true
            ),
            ChatMessageEntity(
                leadId = 1,
                messageText = "Hello Marcos! Yes, we have some slots on Saturday morning. Would 10:00 AM work for you?",
                timestamp = "10:25 AM",
                timestampLong = now - 79 * 60 * 1000,
                isFromClient = false
            ),
            ChatMessageEntity(
                leadId = 1,
                messageText = "Saturday at 10:00 AM sounds perfect. Can you confirm the price for the initial consultation? Also, do you handle the waste removal as well?",
                timestamp = "11:42 AM",
                timestampLong = now - 50 * 60 * 1000,
                isFromClient = true
            )
        )
        chatMessageDao.insertMessages(sampleMessages)

        // Seed default suggestion for Marcos Oliveira (Lead ID = 1)
        val defaultSuggestion = AiSuggestionEntity(
            leadId = 1,
            suggestionText = "Great! The consultation fee is £50, which we credit back if you proceed with the project. And yes, we handle all waste removal and disposal as part of our full-service redesign. Shall I lock in 10:00 AM Saturday for you?",
            matchPercentage = 98,
            reason = "Addresses price consultation and waste disposal inquiries directly."
        )
        aiSuggestionDao.insertSuggestion(defaultSuggestion)

        Log.d(TAG, "Sample data seeding completed successfully!")
    }
}
