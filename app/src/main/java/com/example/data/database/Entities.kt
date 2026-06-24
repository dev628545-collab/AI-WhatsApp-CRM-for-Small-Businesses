package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "leads",
    indices = [Index(value = ["status"]), Index(value = ["pipelineStage"])]
)
data class LeadEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val status: String, // "HOT", "NEW", "FOLLOW-UP", "COLD", "ARCHIVED"
    val assignedTo: String,
    val assignedToInitials: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val lastMessageTimestamp: Long,
    val pipelineStage: String, // "NEW LEAD", "CONTACTED", "QUOTE SENT", "CLOSED WON", "CLOSED LOST"
    val dealValue: Double,
    val leadScore: Int,
    val category: String, // e.g. "Landscaping", "Web Dev", "Branding", "App Build"
    val unread: Boolean = false,
    val imageUrl: String? = null
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = LeadEntity::class,
            parentColumns = ["id"],
            childColumns = ["leadId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["leadId"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val leadId: Int,
    val messageText: String,
    val timestamp: String, // "10:14 AM"
    val timestampLong: Long,
    val isFromClient: Boolean,
    val isRead: Boolean = true
)

@Entity(
    tableName = "ai_suggestions",
    indices = [Index(value = ["leadId"])]
)
data class AiSuggestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val leadId: Int,
    val suggestionText: String,
    val matchPercentage: Int, // e.g. 98
    val reason: String = "",
    val createdTimestamp: Long = System.currentTimeMillis()
)
