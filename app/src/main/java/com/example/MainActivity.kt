package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.AppDatabase
import com.example.data.TimeTrackerRepository
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TimeTrackerViewModel
import com.example.viewmodel.TimeTrackerViewModelFactory
import com.example.workers.ReminderWorker
import java.util.concurrent.TimeUnit

import com.example.ui.theme.grainEffect

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Could react if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Room DB & ViewModel init
        val database = AppDatabase.getDatabase(this)
        val repository = TimeTrackerRepository(database.timeTrackerDao())
        val viewModelFactory = TimeTrackerViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[TimeTrackerViewModel::class.java]

        askNotificationPermission()
        setupWorker()
        com.example.utils.MonthlyReportManager.checkAndGenerateMonthlyReports(this)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .grainEffect()
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupWorker() {
        // Enqueues a worker to run roughly every 12 hours acting as a daily reminder
        val reminderWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyReminderWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderWorkRequest
        )
    }
}
