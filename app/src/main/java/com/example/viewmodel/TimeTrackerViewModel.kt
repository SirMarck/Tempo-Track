package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Client
import com.example.data.Session
import com.example.data.TimeTrackerRepository
import com.example.data.gemini.ClientSummaryData
import com.example.data.gemini.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class GeminiAnalysisState {
    object Idle : GeminiAnalysisState()
    object Loading : GeminiAnalysisState()
    data class Success(val analysis: String) : GeminiAnalysisState()
    data class Error(val message: String) : GeminiAnalysisState()
}

class TimeTrackerViewModel(
    private val repository: TimeTrackerRepository,
    private val geminiRepository: GeminiRepository = GeminiRepository()
) : ViewModel() {

    private val _geminiAnalysisState = MutableStateFlow<GeminiAnalysisState>(GeminiAnalysisState.Idle)
    val geminiAnalysisState: StateFlow<GeminiAnalysisState> = _geminiAnalysisState

    val clients: StateFlow<List<Client>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeClients: StateFlow<List<Client>> = repository.activeClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<com.example.data.Project>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProjects: StateFlow<List<com.example.data.Project>> = repository.activeProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activities: StateFlow<List<com.example.data.Activity>> = repository.activeActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<Session>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSession: StateFlow<Session?> = repository.activeSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val closingBatches: StateFlow<List<com.example.data.ClosingBatch>> = repository.allClosingBatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addClient(name: String, hourlyRate: Double, notes: String? = null) {
        viewModelScope.launch {
            repository.insertClient(Client(name = name, hourlyRate = hourlyRate, notes = notes))
        }
    }

    fun startSession(
        clientId: Long,
        description: String = "",
        tag: String = "",
        projectId: Long? = null,
        activityId: Long? = null,
        billable: Boolean = true
    ) {
        viewModelScope.launch {
            if (activeSession.value == null) {
                val now = System.currentTimeMillis()
                // Snapshot da taxa seguindo: Projeto -> Cliente -> 0.0 (se faturável)
                val project = projectId?.let { repository.getProjectById(it) }
                val client = repository.getClientById(clientId)
                val resolvedRate = project?.hourlyRate?.takeIf { it > 0.0 } ?: client?.hourlyRate ?: 0.0
                val rate = if (billable) resolvedRate else 0.0

                val newSessionId = repository.insertSession(
                    Session(
                        clientId = clientId,
                        projectId = projectId,
                        activityId = activityId,
                        billable = billable,
                        appliedRate = rate,
                        startTime = now,
                        description = description,
                        tag = tag,
                        status = "running",
                        source = "timer"
                    )
                )

                // Cria o primeiro TimeSegment
                repository.insertSegment(
                    com.example.data.TimeSegment(
                        sessionId = newSessionId,
                        startedAt = now
                    )
                )
            }
        }
    }

    fun stopActiveSession() {
        viewModelScope.launch {
            val session = activeSession.value
            if (session != null) {
                val now = System.currentTimeMillis()
                val finalEndTime = if (session.isPaused) (session.lastPausedTime ?: now) else now

                // Encerra qualquer segmento em aberto
                val segments = repository.getSegmentsForSessionSync(session.id)
                val lastOpen = segments.lastOrNull { it.endedAt == null }
                if (lastOpen != null) {
                    repository.updateSegment(lastOpen.copy(endedAt = finalEndTime))
                }

                repository.updateSession(
                    session.copy(
                        endTime = finalEndTime,
                        status = "completed"
                    )
                )
            }
        }
    }

    fun pauseActiveSession() {
        viewModelScope.launch {
            val session = activeSession.value
            if (session != null && !session.isPaused) {
                val now = System.currentTimeMillis()
                val newEvents = if (session.pauseEvents.isEmpty()) "P:$now" else "${session.pauseEvents},P:$now"

                // Encerra o segmento atual com endedAt
                val segments = repository.getSegmentsForSessionSync(session.id)
                val lastOpen = segments.lastOrNull { it.endedAt == null }
                if (lastOpen != null) {
                    repository.updateSegment(lastOpen.copy(endedAt = now))
                }

                repository.updateSession(
                    session.copy(
                        isPaused = true,
                        lastPausedTime = now,
                        pauseEvents = newEvents,
                        status = "paused"
                    )
                )
            }
        }
    }

    fun resumeActiveSession() {
        viewModelScope.launch {
            val session = activeSession.value
            if (session != null && session.isPaused) {
                val now = System.currentTimeMillis()
                val addedPause = now - (session.lastPausedTime ?: now)
                val newEvents = if (session.pauseEvents.isEmpty()) "R:$now" else "${session.pauseEvents},R:$now"

                // Cria novo segmento de tempo para o trecho retomado
                repository.insertSegment(
                    com.example.data.TimeSegment(
                        sessionId = session.id,
                        startedAt = now
                    )
                )

                repository.updateSession(
                    session.copy(
                        isPaused = false,
                        lastPausedTime = null,
                        pausedDuration = session.pausedDuration + addedPause,
                        pauseEvents = newEvents,
                        status = "running"
                    )
                )
            }
        }
    }

    fun updateClient(client: Client) {
        viewModelScope.launch {
            repository.updateClient(client)
        }
    }

    fun deleteClient(id: Long) {
        viewModelScope.launch {
            // Regra do Guia v2: se houver sessões associadas, arquiva em vez de deletar fisicamente
            val count = repository.countSessionsForClient(id)
            if (count > 0) {
                repository.archiveClient(id)
            } else {
                repository.deleteClientById(id)
            }
        }
    }

    fun archiveClient(id: Long) {
        viewModelScope.launch {
            repository.archiveClient(id)
        }
    }

    fun unarchiveClient(id: Long) {
        viewModelScope.launch {
            repository.unarchiveClient(id)
        }
    }

    // ─── Projetos ─────────────────────────────────────────────────────────────
    fun addProject(
        clientId: Long,
        name: String,
        billingMode: String = "hourly",
        hourlyRate: Double? = null,
        budgetMinutes: Long? = null,
        budgetAmount: Double? = null
    ) {
        viewModelScope.launch {
            repository.insertProject(
                com.example.data.Project(
                    clientId = clientId,
                    name = name,
                    billingMode = billingMode,
                    hourlyRate = hourlyRate,
                    budgetMinutes = budgetMinutes,
                    budgetAmount = budgetAmount
                )
            )
        }
    }

    fun updateProject(project: com.example.data.Project) {
        viewModelScope.launch {
            repository.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun archiveProject(id: Long) {
        viewModelScope.launch {
            repository.archiveProject(id)
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            val count = repository.countSessionsForProject(id)
            if (count > 0) {
                repository.archiveProject(id)
            } else {
                repository.deleteProjectById(id)
            }
        }
    }

    // ─── Atividades ───────────────────────────────────────────────────────────
    fun addActivity(name: String, defaultBillable: Boolean = true) {
        viewModelScope.launch {
            repository.insertActivity(com.example.data.Activity(name = name, defaultBillable = defaultBillable))
        }
    }

    fun updateActivity(activity: com.example.data.Activity) {
        viewModelScope.launch {
            repository.updateActivity(activity)
        }
    }

    fun deleteActivity(id: Long) {
        viewModelScope.launch {
            repository.deleteActivityById(id)
        }
    }

    fun archiveActivity(id: Long) {
        viewModelScope.launch {
            repository.archiveActivity(id)
        }
    }

    // ─── Tags Personalizáveis ──────────────────────────────────────────────────
    private val _tags = MutableStateFlow<List<String>>(com.example.utils.TagManager.DEFAULT_TAGS)
    val tags: StateFlow<List<String>> = _tags

    fun loadTags(context: android.content.Context) {
        _tags.value = com.example.utils.TagManager.getTags(context)
    }

    fun addTag(context: android.content.Context, tag: String) {
        _tags.value = com.example.utils.TagManager.addTag(context, tag)
    }

    fun updateTag(context: android.content.Context, oldTag: String, newTag: String) {
        _tags.value = com.example.utils.TagManager.updateTag(context, oldTag, newTag)
    }

    fun deleteTag(context: android.content.Context, tag: String) {
        _tags.value = com.example.utils.TagManager.deleteTag(context, tag)
    }

    // ─── Fechamentos Financeiros ──────────────────────────────────────────────
    fun createClosingBatch(
        clientId: Long,
        fromDate: Long,
        toDate: Long,
        sessionsToClose: List<Session>,
        invoiceNumber: String? = null,
        notes: String? = null,
        onComplete: ((Long) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val client = repository.getClientById(clientId)
            val rateFallback = client?.hourlyRate ?: 0.0
            val totalHours = sessionsToClose.sumOf { it.calculateDurationMillis().toDouble() / (1000 * 60 * 60) }
            val totalAmount = sessionsToClose.sumOf { it.calculateEarnings(rateFallback) }

            val batchId = repository.insertClosingBatch(
                com.example.data.ClosingBatch(
                    clientId = clientId,
                    fromDate = fromDate,
                    toDate = toDate,
                    status = "invoiced",
                    totalHours = totalHours,
                    totalAmount = totalAmount,
                    sessionCount = sessionsToClose.size,
                    invoiceNumber = invoiceNumber,
                    notes = notes
                )
            )

            val entries = sessionsToClose.map { session ->
                com.example.data.ClosingEntry(closingBatchId = batchId, sessionId = session.id)
            }
            repository.insertClosingEntries(entries)

            // Atualiza o estado de cada sessão para invoiced e associa ao batch
            for (session in sessionsToClose) {
                repository.updateSessionClosing(session.id, batchId, "invoiced")
            }

            onComplete?.invoke(batchId)
        }
    }

    fun markBatchAsPaid(batchId: Long) {
        viewModelScope.launch {
            repository.updateClosingBatchStatus(batchId, "paid")
        }
    }

    fun updateSession(session: Session) {
        viewModelScope.launch {
            repository.updateSession(session)
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            repository.deleteSessionById(id)
        }
    }

    fun addManualSession(
        clientId: Long,
        startTime: Long,
        endTime: Long,
        description: String,
        discountValue: Double = 0.0,
        discountPercentage: Double = 0.0,
        tag: String = "",
        projectId: Long? = null,
        activityId: Long? = null,
        billable: Boolean = true,
        customRate: Double? = null
    ) {
        viewModelScope.launch {
            val client = repository.getClientById(clientId)
            val project = projectId?.let { repository.getProjectById(it) }
            val rate = customRate ?: project?.hourlyRate ?: client?.hourlyRate ?: 0.0

            val sessionId = repository.insertSession(
                Session(
                    clientId = clientId,
                    projectId = projectId,
                    activityId = activityId,
                    startTime = startTime,
                    endTime = endTime,
                    description = description,
                    discountValue = discountValue,
                    discountPercentage = discountPercentage,
                    tag = tag,
                    billable = billable,
                    appliedRate = rate,
                    status = "completed",
                    source = "manual"
                )
            )

            // Salva o segmento do lançamento manual
            repository.insertSegment(
                com.example.data.TimeSegment(
                    sessionId = sessionId,
                    startedAt = startTime,
                    endedAt = endTime
                )
            )
        }
    }


    fun requestMonthlyAnalysis(
        monthName: String,
        totalEarnings: Double,
        totalHours: Double,
        clientSummaries: List<ClientSummaryData>
    ) {
        viewModelScope.launch {
            _geminiAnalysisState.value = GeminiAnalysisState.Loading
            val result = geminiRepository.analyzeMonthlyReport(
                monthName = monthName,
                totalEarnings = totalEarnings,
                totalHours = totalHours,
                clientSummaries = clientSummaries
            )
            result.onSuccess { analysis ->
                _geminiAnalysisState.value = GeminiAnalysisState.Success(analysis)
            }.onFailure { error ->
                _geminiAnalysisState.value = GeminiAnalysisState.Error(
                    error.localizedMessage ?: "Erro ao comunicar com a IA do Gemini."
                )
            }
        }
    }

    fun clearGeminiAnalysis() {
        _geminiAnalysisState.value = GeminiAnalysisState.Idle
    }

    fun clearGeminiState() {
        _geminiAnalysisState.value = GeminiAnalysisState.Idle
    }

    fun parseQuickSessionWithGemini(
        text: String,
        onResult: (com.example.data.gemini.ParsedQuickSession?, String?) -> Unit
    ) {
        viewModelScope.launch {
            val available = clients.value
            val result = geminiRepository.parseSessionFromNaturalText(text, available)
            result.onSuccess { parsed ->
                onResult(parsed, null)
            }.onFailure { error ->
                onResult(null, error.localizedMessage ?: "Erro ao processar frase com Gemini.")
            }
        }
    }

    /**
     * Gera um arquivo JSON de backup e o compartilha via sharesheet do Android.
     * @param context contexto necessário para criar o arquivo e abrir o chooser
     */
    fun exportAndShareBackup(context: android.content.Context) {
        viewModelScope.launch {
            val file = com.example.utils.BackupManager.exportBackup(
                context = context,
                clients = clients.value,
                sessions = sessions.value
            )
            if (file != null) {
                com.example.utils.BackupManager.shareBackup(context, file)
            }
        }
    }

    /**
     * Lê o JSON de um URI (arquivo escolhido pelo usuário) e restaura dados.
     * @param context contexto para ler o arquivo
     * @param uri URI retornado pelo ActivityResultLauncher do FILE_OPEN picker
     * @param onComplete callback com o resultado do restore (sucesso ou erro)
     */
    fun restoreBackup(
        context: android.content.Context,
        uri: android.net.Uri,
        onComplete: (com.example.utils.BackupResult) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    ?: return@launch onComplete(com.example.utils.BackupResult.Error("Não foi possível ler o arquivo."))
                com.example.utils.BackupManager.parseAndRestore(context, jsonString, repository, onComplete)
            } catch (e: Exception) {
                onComplete(com.example.utils.BackupResult.Error("Erro ao abrir o arquivo: ${e.localizedMessage}"))
            }
        }
    }

    fun createClosingBatch(
        clientId: Long,
        fromDate: Long,
        toDate: Long,
        sessionIds: List<Long>,
        notes: String? = null,
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val sessionsToClose = sessionIds.mapNotNull { repository.getSessionById(it) }
            val client = repository.getClientById(clientId)
            val fallbackRate = client?.hourlyRate ?: 0.0

            val totalDurationMillis = sessionsToClose.sumOf { it.calculateDurationMillis() }
            val totalHours = totalDurationMillis.toDouble() / (1000 * 60 * 60)
            val totalAmount = sessionsToClose.sumOf { it.calculateEarnings(fallbackRate) }

            val batch = com.example.data.ClosingBatch(
                clientId = clientId,
                fromDate = fromDate,
                toDate = toDate,
                status = "invoiced",
                totalHours = totalHours,
                totalAmount = totalAmount,
                sessionCount = sessionsToClose.size,
                notes = notes,
                createdAt = System.currentTimeMillis()
            )

            val batchId = repository.insertClosingBatch(batch)

            // Insere as entradas de ligação e atualiza as sessões
            val entries = sessionIds.map { com.example.data.ClosingEntry(closingBatchId = batchId, sessionId = it) }
            repository.insertClosingEntries(entries)

            sessionIds.forEach { sid ->
                repository.updateSessionClosing(sid, batchId, "billed")
            }

            onComplete(batchId)
        }
    }

    fun markClosingBatchPaid(batchId: Long) {
        viewModelScope.launch {
            repository.updateClosingBatchStatus(batchId, "paid")
        }
    }
}

class TimeTrackerViewModelFactory(
    private val repository: TimeTrackerRepository,
    private val geminiRepository: GeminiRepository = GeminiRepository()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimeTrackerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimeTrackerViewModel(repository, geminiRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
