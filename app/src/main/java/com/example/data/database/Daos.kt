package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LeadDao {
    @Query("SELECT * FROM leads ORDER BY lastMessageTimestamp DESC")
    fun getAllLeads(): Flow<List<LeadEntity>>

    @Query("SELECT * FROM leads WHERE id = :id LIMIT 1")
    fun getLeadByIdFlow(id: Int): Flow<LeadEntity?>

    @Query("SELECT * FROM leads WHERE id = :id LIMIT 1")
    suspend fun getLeadById(id: Int): LeadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: LeadEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeads(leads: List<LeadEntity>)

    @Update
    suspend fun updateLead(lead: LeadEntity)

    @Delete
    suspend fun deleteLead(lead: LeadEntity)

    @Query("UPDATE leads SET pipelineStage = :stage WHERE id = :leadId")
    suspend fun updateLeadPipelineStage(leadId: Int, stage: String)

    @Query("UPDATE leads SET status = :status WHERE id = :leadId")
    suspend fun updateLeadStatus(leadId: Int, status: String)

    @Query("UPDATE leads SET lastMessage = :message, lastMessageTime = :time, lastMessageTimestamp = :timestamp WHERE id = :leadId")
    suspend fun updateLeadLastMessage(leadId: Int, message: String, time: String, timestamp: Long)

    @Query("UPDATE leads SET leadScore = :score WHERE id = :leadId")
    suspend fun updateLeadScore(leadId: Int, score: Int)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE leadId = :leadId ORDER BY timestampLong ASC")
    fun getMessagesForLead(leadId: Int): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages WHERE leadId = :leadId")
    suspend fun deleteMessagesForLead(leadId: Int)
}

@Dao
interface AiSuggestionDao {
    @Query("SELECT * FROM ai_suggestions WHERE leadId = :leadId ORDER BY createdTimestamp DESC LIMIT 1")
    fun getSuggestionForLead(leadId: Int): Flow<AiSuggestionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestion(suggestion: AiSuggestionEntity)

    @Query("DELETE FROM ai_suggestions WHERE leadId = :leadId")
    suspend fun deleteSuggestionForLead(leadId: Int)
}
