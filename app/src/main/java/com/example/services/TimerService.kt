package com.example.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.Session
import com.example.data.TimeTrackerRepository
import com.example.utils.FormatUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull

class TimerService : Service() {

    companion object {
        const val NOTIFICATION_ID = 8888
        const val CHANNEL_ID = "active_timer_channel"

        const val ACTION_START = "com.example.action.START"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_RESUME = "com.example.action.RESUME"
        const val ACTION_STOP = "com.example.action.STOP"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var repository: TimeTrackerRepository
    private lateinit var notificationManager: NotificationManager

    private var activeSessionJob: Job? = null
    private var tickJob: Job? = null
    private var currentSession: Session? = null
    private var currentClientName: String = "Cliente"

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        val database = AppDatabase.getDatabase(applicationContext)
        repository = TimeTrackerRepository(database.timeTrackerDao())

        // Automatically observe the active session flow from database
        observeActiveSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> pauseSession()
            ACTION_RESUME -> resumeSession()
            ACTION_STOP -> stopSessionAndOpenApp()
            ACTION_START -> {
                // Handled by flow observation, but ensures service is alive
            }
        }
        return START_STICKY
    }

    private fun observeActiveSession() {
        activeSessionJob = serviceScope.launch {
            repository.activeSession.collect { session ->
                if (session == null) {
                    // No active session, stop service
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    currentSession = session
                    val clientName = repository.getClientById(session.clientId)?.name ?: "Cliente"
                    currentClientName = clientName
                    handleSessionUpdate(session, clientName)
                }
            }
        }
    }

    private fun handleSessionUpdate(session: Session, clientName: String) {
        tickJob?.cancel()

        if (session.isPaused) {
            // Static pause notification
            val lastPaused = session.lastPausedTime ?: System.currentTimeMillis()
            val duration = maxOf(0L, lastPaused - session.startTime - session.pausedDuration)
            val durationText = FormatUtils.formatDuration(duration)
            val notification = buildNotification(session, clientName, durationText)
            
            startForegroundCompat(notification)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } else {
            // Dynamic ticking notification
            tickJob = serviceScope.launch {
                while (isActive) {
                    val now = System.currentTimeMillis()
                    val duration = maxOf(0L, now - session.startTime - session.pausedDuration)
                    val durationText = FormatUtils.formatDuration(duration)
                    val notification = buildNotification(session, clientName, durationText)
                    
                    startForegroundCompat(notification)
                    notificationManager.notify(NOTIFICATION_ID, notification)
                    delay(1000)
                }
            }
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(
        session: Session,
        clientName: String,
        durationText: String
    ): Notification {
        val title = if (session.isPaused) "Trabalho Pausado" else "Trabalho em Andamento"
        val contentText = "$clientName • $durationText"

        // Open app when notification clicked
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification builder
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play) // Standard system icon representing the active timer
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        // Actions
        if (session.isPaused) {
            val resumeIntent = Intent(this, TimerService::class.java).apply { action = ACTION_RESUME }
            val resumePendingIntent = PendingIntent.getService(
                this,
                1,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Retomar",
                resumePendingIntent
            )
        } else {
            val pauseIntent = Intent(this, TimerService::class.java).apply { action = ACTION_PAUSE }
            val pausePendingIntent = PendingIntent.getService(
                this,
                2,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Pausar",
                pausePendingIntent
            )
        }

        val stopIntent = Intent(this, TimerService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this,
            3,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Encerrar",
            stopPendingIntent
        )

        return builder.build()
    }

    private fun pauseSession() {
        serviceScope.launch {
            val session = repository.activeSession.firstOrNull()
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

    private fun resumeSession() {
        serviceScope.launch {
            val session = repository.activeSession.firstOrNull()
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

    private fun stopSessionAndOpenApp() {
        serviceScope.launch {
            val session = repository.activeSession.firstOrNull()
            if (session != null) {
                val now = System.currentTimeMillis()
                val finalEndTime = if (session.isPaused) (session.lastPausedTime ?: now) else now
                repository.updateSession(session.copy(endTime = finalEndTime))
            }

            // Open app
            val mainIntent = Intent(this@TimerService, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(mainIntent)

            // Stop service
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Tempo de Trabalho Ativo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mostra o cronômetro ativo e controles de pausa/encerramento do trabalho atual."
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tickJob?.cancel()
        activeSessionJob?.cancel()
        serviceJob.cancel()
        super.onDestroy()
    }
}
