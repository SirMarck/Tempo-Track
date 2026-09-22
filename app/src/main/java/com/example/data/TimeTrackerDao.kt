package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeTrackerDao {

    // ─── Clientes ─────────────────────────────────────────────────────────────
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE archivedAt IS NULL ORDER BY name ASC")
    fun getActiveClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients ORDER BY name ASC")
    suspend fun getAllClientsSync(): List<Client>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientById(id: Long): Client?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Query("UPDATE clients SET archivedAt = :archivedAt WHERE id = :id")
    suspend fun archiveClient(id: Long, archivedAt: Long = System.currentTimeMillis())

    @Query("UPDATE clients SET archivedAt = NULL WHERE id = :id")
    suspend fun unarchiveClient(id: Long)

    @Query("SELECT COUNT(*) FROM sessions WHERE clientId = :clientId")
    suspend fun countSessionsForClient(clientId: Long): Int

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun deleteClientById(id: Long)

    // ─── Projetos ─────────────────────────────────────────────────────────────
    @Query("SELECT * FROM projects ORDER BY name ASC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE archivedAt IS NULL ORDER BY name ASC")
    fun getActiveProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE clientId = :clientId AND archivedAt IS NULL ORDER BY name ASC")
    fun getProjectsForClient(clientId: Long): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Query("UPDATE projects SET archivedAt = :archivedAt WHERE id = :id")
    suspend fun archiveProject(id: Long, archivedAt: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET archivedAt = NULL WHERE id = :id")
    suspend fun unarchiveProject(id: Long)

    @Query("SELECT COUNT(*) FROM sessions WHERE projectId = :projectId")
    suspend fun countSessionsForProject(projectId: Long): Int

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    // ─── Atividades ───────────────────────────────────────────────────────────
    @Query("SELECT * FROM activities WHERE archivedAt IS NULL ORDER BY name ASC")
    fun getActiveActivities(): Flow<List<Activity>>

    @Query("SELECT * FROM activities ORDER BY name ASC")
    fun getAllActivities(): Flow<List<Activity>>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityById(id: Long): Activity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: Activity): Long

    @Update
    suspend fun updateActivity(activity: Activity)

    @Query("UPDATE activities SET archivedAt = :archivedAt WHERE id = :id")
    suspend fun archiveActivity(id: Long, archivedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivityById(id: Long)

    // ─── Sessões (WorkSession) ────────────────────────────────────────────────
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    suspend fun getAllSessionsSync(): List<Session>

    @Query("SELECT * FROM sessions WHERE clientId = :clientId ORDER BY startTime DESC")
    fun getSessionsForClient(clientId: Long): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE projectId = :projectId ORDER BY startTime DESC")
    fun getSessionsForProject(projectId: Long): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE endTime IS NULL LIMIT 1")
    fun getActiveSession(): Flow<Session?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): Session?

    @Query("SELECT * FROM sessions WHERE clientId = :clientId AND financialStatus = 'unbilled' AND endTime IS NOT NULL ORDER BY startTime ASC")
    fun getUnbilledSessionsForClient(clientId: Long): Flow<List<Session>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session): Long

    @Update
    suspend fun updateSession(session: Session)

    @Query("UPDATE sessions SET closingBatchId = :batchId, financialStatus = :financialStatus WHERE id = :sessionId")
    suspend fun updateSessionClosing(sessionId: Long, batchId: Long?, financialStatus: String)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    // ─── Segmentos de Tempo ───────────────────────────────────────────────────
    @Query("SELECT * FROM time_segments WHERE sessionId = :sessionId ORDER BY startedAt ASC")
    fun getSegmentsForSession(sessionId: Long): Flow<List<TimeSegment>>

    @Query("SELECT * FROM time_segments WHERE sessionId = :sessionId ORDER BY startedAt ASC")
    suspend fun getSegmentsForSessionSync(sessionId: Long): List<TimeSegment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegment(segment: TimeSegment): Long

    @Update
    suspend fun updateSegment(segment: TimeSegment)

    @Query("DELETE FROM time_segments WHERE sessionId = :sessionId")
    suspend fun deleteSegmentsForSession(sessionId: Long)

    // ─── Fechamentos (ClosingBatch & ClosingEntry) ────────────────────────────
    @Query("SELECT * FROM closing_batches ORDER BY createdAt DESC")
    fun getAllClosingBatches(): Flow<List<ClosingBatch>>

    @Query("SELECT * FROM closing_batches WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun getClosingBatchesForClient(clientId: Long): Flow<List<ClosingBatch>>

    @Query("SELECT * FROM closing_batches WHERE id = :id")
    suspend fun getClosingBatchById(id: Long): ClosingBatch?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClosingBatch(batch: ClosingBatch): Long

    @Update
    suspend fun updateClosingBatch(batch: ClosingBatch)

    @Query("UPDATE closing_batches SET status = :status WHERE id = :id")
    suspend fun updateClosingBatchStatus(id: Long, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClosingEntries(entries: List<ClosingEntry>)

    @Query("SELECT * FROM closing_entries WHERE closingBatchId = :batchId")
    fun getEntriesForBatch(batchId: Long): Flow<List<ClosingEntry>>

    @Query("SELECT * FROM sessions WHERE id IN (SELECT sessionId FROM closing_entries WHERE closingBatchId = :batchId)")
    fun getSessionsInBatch(batchId: Long): Flow<List<Session>>

    @Query("DELETE FROM closing_entries WHERE closingBatchId = :batchId")
    suspend fun deleteEntriesForBatch(batchId: Long)

    @Query("DELETE FROM closing_batches WHERE id = :id")
    suspend fun deleteClosingBatchById(id: Long)
}

