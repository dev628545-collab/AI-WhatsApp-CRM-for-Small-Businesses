package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.ChatMessageEntity
import com.example.data.database.LeadEntity
import com.example.data.repository.CrmRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrmViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "CrmViewModel"
    
    private val db = AppDatabase.getDatabase(application)
    private val repository = CrmRepository(
        leadDao = db.leadDao(),
        chatMessageDao = db.chatMessageDao(),
        aiSuggestionDao = db.aiSuggestionDao()
    )

    // Filter state for the Inbox
    enum class FilterType { ALL, UNREAD, HOT, ARCHIVED }
    private val _inboxFilter = MutableStateFlow(FilterType.ALL)
    val inboxFilter: StateFlow<FilterType> = _inboxFilter.asStateFlow()

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // All active leads from DB
    val leads: StateFlow<List<LeadEntity>> = repository.allLeads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered and searched leads for the Inbox screen
    val filteredLeads: StateFlow<List<LeadEntity>> = combine(
        leads,
        _inboxFilter,
        _searchQuery
    ) { leadsList, filter, query ->
        var filtered = leadsList.filter { lead ->
            when (filter) {
                FilterType.ALL -> lead.status != "ARCHIVED"
                FilterType.UNREAD -> lead.status != "ARCHIVED" && (lead.unread || lead.status == "NEW")
                FilterType.HOT -> lead.status == "HOT"
                FilterType.ARCHIVED -> lead.status == "ARCHIVED"
            }
        }
        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.phone.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
            }
        }
        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current selected lead ID for the Detail Screen
    private val _selectedLeadId = MutableStateFlow<Int?>(null)
    val selectedLeadId: StateFlow<Int?> = _selectedLeadId.asStateFlow()

    // Selected lead details flow
    val selectedLead: StateFlow<LeadEntity?> = _selectedLeadId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getLeadById(id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Messages flow for the selected lead
    val activeMessages: StateFlow<List<ChatMessageEntity>> = _selectedLeadId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getMessagesForLead(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // AI Suggestions flow for the selected lead
    val activeAiSuggestion = _selectedLeadId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getSuggestionForLead(id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // AI loading state
    private val _isGeneratingAi = MutableStateFlow(false)
    val isGeneratingAi: StateFlow<Boolean> = _isGeneratingAi.asStateFlow()

    init {
        // Run database seeding and make sure default items exist on first run
        viewModelScope.launch {
            try {
                repository.seedDefaultDataIfEmpty()
            } catch (e: Exception) {
                Log.e(TAG, "Error seeding default data", e)
            }
        }
    }

    fun selectLead(leadId: Int?) {
        _selectedLeadId.value = leadId
        if (leadId != null) {
            // Mark as read when viewing
            viewModelScope.launch {
                leads.value.find { it.id == leadId }?.let { lead ->
                    if (lead.unread) {
                        repository.updateLead(lead.copy(unread = false))
                    }
                }
            }
        }
    }

    fun setInboxFilter(filter: FilterType) {
        _inboxFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Add a completely new lead and its initial message.
     */
    fun addNewLead(
        name: String,
        phone: String,
        category: String,
        dealValue: Double,
        status: String = "NEW",
        initialMessage: String = ""
    ) {
        viewModelScope.launch {
            try {
                val initials = name.split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .take(2)
                    .joinToString("")

                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

                val newLead = LeadEntity(
                    name = name,
                    phone = phone,
                    status = status,
                    assignedTo = "Me (You)",
                    assignedToInitials = "ME",
                    lastMessage = initialMessage.ifEmpty { "New Lead Added" },
                    lastMessageTime = timeStr,
                    lastMessageTimestamp = System.currentTimeMillis(),
                    pipelineStage = "NEW LEAD",
                    dealValue = dealValue,
                    leadScore = 70, // Default baseline score
                    category = category
                )

                val leadId = repository.insertLead(newLead).toInt()

                if (initialMessage.isNotEmpty()) {
                    repository.insertMessage(
                        ChatMessageEntity(
                            leadId = leadId,
                            messageText = initialMessage,
                            timestamp = timeStr,
                            timestampLong = System.currentTimeMillis(),
                            isFromClient = true
                        )
                    )
                    // Trigger AI to respond
                    triggerAiSuggestion(leadId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add lead", e)
            }
        }
    }

    /**
     * Send/Receive a message in a conversation.
     */
    fun sendMessage(leadId: Int, text: String, isFromClient: Boolean) {
        viewModelScope.launch {
            try {
                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                repository.insertMessage(
                    ChatMessageEntity(
                        leadId = leadId,
                        messageText = text,
                        timestamp = timeStr,
                        timestampLong = System.currentTimeMillis(),
                        isFromClient = isFromClient
                    )
                )

                // If message is from client, automatically trigger Gemini suggestion
                if (isFromClient) {
                    triggerAiSuggestion(leadId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
            }
        }
    }

    /**
     * Trigger generation of a fresh AI suggestion manually.
     */
    fun triggerAiSuggestion(leadId: Int) {
        viewModelScope.launch {
            _isGeneratingAi.value = true
            try {
                repository.generateAiSuggestion(leadId)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating AI Suggestion", e)
            } finally {
                _isGeneratingAi.value = false
            }
        }
    }

    fun updatePipelineStage(leadId: Int, stage: String) {
        viewModelScope.launch {
            try {
                repository.updateLeadPipelineStage(leadId, stage)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update pipeline stage", e)
            }
        }
    }

    fun updateLeadStatus(leadId: Int, status: String) {
        viewModelScope.launch {
            try {
                repository.updateLeadStatus(leadId, status)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update lead status", e)
            }
        }
    }

    fun deleteLead(lead: LeadEntity) {
        viewModelScope.launch {
            try {
                repository.deleteLead(lead)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete lead", e)
            }
        }
    }
}

class CrmViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CrmViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CrmViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
