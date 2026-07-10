package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Client
import com.example.data.Session
import com.example.data.TimeTrackerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimeTrackerViewModel(private val repository: TimeTrackerRepository) : ViewModel() {

    val clients: StateFlow<List<Client>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<Session>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSession: StateFlow<Session?> = repository.activeSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addClient(name: String, hourlyRate: Double) {
        viewModelScope.launch {
            repository.insertClient(Client(name = name, hourlyRate = hourlyRate))
        }
    }

    fun startSession(clientId: Long, description: String) {
        viewModelScope.launch {
            // First check if there's any active session, shouldn't start 2
            if (activeSession.value == null) {
                repository.insertSession(
                    Session(
                        clientId = clientId,
                        startTime = System.currentTimeMillis(),
                        description = description
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
                if (session.isPaused) {
                    val finalEndTime = session.lastPausedTime ?: now
                    repository.updateSession(session.copy(endTime = finalEndTime))
                } else {
                    repository.updateSession(session.copy(endTime = now))
                }
            }
        }
    }

    fun pauseActiveSession() {
        viewModelScope.launch {
            val session = activeSession.value
            if (session != null && !session.isPaused) {
                val now = System.currentTimeMillis()
                val newEvents = if (session.pauseEvents.isEmpty()) "P:$now" else "${session.pauseEvents},P:$now"
                repository.updateSession(
                    session.copy(
                        isPaused = true,
                        lastPausedTime = now,
                        pauseEvents = newEvents
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
                repository.updateSession(
                    session.copy(
                        isPaused = false,
                        lastPausedTime = null,
                        pausedDuration = session.pausedDuration + addedPause,
                        pauseEvents = newEvents
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
            repository.deleteClientById(id)
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
        discountPercentage: Double = 0.0
    ) {
        viewModelScope.launch {
            repository.insertSession(
                Session(
                    clientId = clientId,
                    startTime = startTime,
                    endTime = endTime,
                    description = description,
                    discountValue = discountValue,
                    discountPercentage = discountPercentage
                )
            )
        }
    }
}

class TimeTrackerViewModelFactory(private val repository: TimeTrackerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimeTrackerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimeTrackerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
