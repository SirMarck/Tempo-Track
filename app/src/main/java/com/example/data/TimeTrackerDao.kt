package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeTrackerDao {
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients ORDER BY name ASC")
    suspend fun getAllClientsSync(): List<Client>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientById(id: Long): Client?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client)

    @Update
    suspend fun updateClient(client: Client)

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun deleteClientById(id: Long)

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    suspend fun getAllSessionsSync(): List<Session>

    @Query("SELECT * FROM sessions WHERE clientId = :clientId ORDER BY startTime DESC")
    fun getSessionsForClient(clientId: Long): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE endTime IS NULL LIMIT 1")
    fun getActiveSession(): Flow<Session?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session)

    @Update
    suspend fun updateSession(session: Session)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}
