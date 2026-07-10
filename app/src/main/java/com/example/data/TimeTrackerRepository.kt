package com.example.data

import kotlinx.coroutines.flow.Flow

class TimeTrackerRepository(private val dao: TimeTrackerDao) {
    val allClients: Flow<List<Client>> = dao.getAllClients()
    val allSessions: Flow<List<Session>> = dao.getAllSessions()
    val activeSession: Flow<Session?> = dao.getActiveSession()

    suspend fun getAllClientsSync(): List<Client> = dao.getAllClientsSync()
    suspend fun getAllSessionsSync(): List<Session> = dao.getAllSessionsSync()

    fun getSessionsForClient(clientId: Long): Flow<List<Session>> = dao.getSessionsForClient(clientId)

    suspend fun getClientById(id: Long): Client? = dao.getClientById(id)
    suspend fun insertClient(client: Client) = dao.insertClient(client)
    suspend fun updateClient(client: Client) = dao.updateClient(client)
    suspend fun deleteClientById(id: Long) = dao.deleteClientById(id)

    suspend fun insertSession(session: Session) = dao.insertSession(session)
    suspend fun updateSession(session: Session) = dao.updateSession(session)
    suspend fun deleteSessionById(id: Long) = dao.deleteSessionById(id)
}
