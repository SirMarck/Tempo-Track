package com.example.data

import kotlinx.coroutines.flow.Flow

class TimeTrackerRepository(private val dao: TimeTrackerDao) {
    // ─── Clientes ─────────────────────────────────────────────────────────────
    val allClients: Flow<List<Client>> = dao.getAllClients()
    val activeClients: Flow<List<Client>> = dao.getActiveClients()

    suspend fun getAllClientsSync(): List<Client> = dao.getAllClientsSync()
    suspend fun getClientById(id: Long): Client? = dao.getClientById(id)
    suspend fun insertClient(client: Client): Long = dao.insertClient(client)
    suspend fun updateClient(client: Client) = dao.updateClient(client)
    suspend fun archiveClient(id: Long) = dao.archiveClient(id)
    suspend fun unarchiveClient(id: Long) = dao.unarchiveClient(id)
    suspend fun countSessionsForClient(clientId: Long): Int = dao.countSessionsForClient(clientId)
    suspend fun deleteClientById(id: Long) = dao.deleteClientById(id)

    // ─── Projetos ─────────────────────────────────────────────────────────────
    val allProjects: Flow<List<Project>> = dao.getAllProjects()
    val activeProjects: Flow<List<Project>> = dao.getActiveProjects()

    fun getProjectsForClient(clientId: Long): Flow<List<Project>> = dao.getProjectsForClient(clientId)
    suspend fun getProjectById(id: Long): Project? = dao.getProjectById(id)
    suspend fun insertProject(project: Project): Long = dao.insertProject(project)
    suspend fun updateProject(project: Project) = dao.updateProject(project)
    suspend fun archiveProject(id: Long) = dao.archiveProject(id)
    suspend fun unarchiveProject(id: Long) = dao.unarchiveProject(id)
    suspend fun countSessionsForProject(projectId: Long): Int = dao.countSessionsForProject(projectId)
    suspend fun deleteProjectById(id: Long) = dao.deleteProjectById(id)

    // ─── Atividades ───────────────────────────────────────────────────────────
    val activeActivities: Flow<List<Activity>> = dao.getActiveActivities()
    val allActivities: Flow<List<Activity>> = dao.getAllActivities()

    suspend fun getActivityById(id: Long): Activity? = dao.getActivityById(id)
    suspend fun insertActivity(activity: Activity): Long = dao.insertActivity(activity)
    suspend fun updateActivity(activity: Activity) = dao.updateActivity(activity)
    suspend fun archiveActivity(id: Long) = dao.archiveActivity(id)
    suspend fun deleteActivityById(id: Long) = dao.deleteActivityById(id)

    // ─── Sessões (WorkSession) ────────────────────────────────────────────────
    val allSessions: Flow<List<Session>> = dao.getAllSessions()
    val activeSession: Flow<Session?> = dao.getActiveSession()

    suspend fun getAllSessionsSync(): List<Session> = dao.getAllSessionsSync()
    fun getSessionsForClient(clientId: Long): Flow<List<Session>> = dao.getSessionsForClient(clientId)
    fun getSessionsForProject(projectId: Long): Flow<List<Session>> = dao.getSessionsForProject(projectId)
    fun getUnbilledSessionsForClient(clientId: Long): Flow<List<Session>> = dao.getUnbilledSessionsForClient(clientId)
    suspend fun getSessionById(id: Long): Session? = dao.getSessionById(id)

    suspend fun insertSession(session: Session): Long = dao.insertSession(session)
    suspend fun updateSession(session: Session) = dao.updateSession(session)
    suspend fun updateSessionClosing(sessionId: Long, batchId: Long?, financialStatus: String) =
        dao.updateSessionClosing(sessionId, batchId, financialStatus)
    suspend fun deleteSessionById(id: Long) = dao.deleteSessionById(id)

    // ─── Segmentos de Tempo ───────────────────────────────────────────────────
    fun getSegmentsForSession(sessionId: Long): Flow<List<TimeSegment>> = dao.getSegmentsForSession(sessionId)
    suspend fun getSegmentsForSessionSync(sessionId: Long): List<TimeSegment> = dao.getSegmentsForSessionSync(sessionId)
    suspend fun insertSegment(segment: TimeSegment): Long = dao.insertSegment(segment)
    suspend fun updateSegment(segment: TimeSegment) = dao.updateSegment(segment)
    suspend fun deleteSegmentsForSession(sessionId: Long) = dao.deleteSegmentsForSession(sessionId)

    // ─── Fechamentos (ClosingBatch & ClosingEntry) ────────────────────────────
    val allClosingBatches: Flow<List<ClosingBatch>> = dao.getAllClosingBatches()
    fun getClosingBatchesForClient(clientId: Long): Flow<List<ClosingBatch>> = dao.getClosingBatchesForClient(clientId)
    suspend fun getClosingBatchById(id: Long): ClosingBatch? = dao.getClosingBatchById(id)
    suspend fun insertClosingBatch(batch: ClosingBatch): Long = dao.insertClosingBatch(batch)
    suspend fun updateClosingBatch(batch: ClosingBatch) = dao.updateClosingBatch(batch)
    suspend fun updateClosingBatchStatus(id: Long, status: String) = dao.updateClosingBatchStatus(id, status)
    suspend fun insertClosingEntries(entries: List<ClosingEntry>) = dao.insertClosingEntries(entries)
    fun getEntriesForBatch(batchId: Long): Flow<List<ClosingEntry>> = dao.getEntriesForBatch(batchId)
    fun getSessionsInBatch(batchId: Long): Flow<List<Session>> = dao.getSessionsInBatch(batchId)
    suspend fun deleteClosingBatchById(id: Long) = dao.deleteClosingBatchById(id)
}

