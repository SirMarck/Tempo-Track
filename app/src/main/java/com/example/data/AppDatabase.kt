package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Client::class,
        Session::class,
        Project::class,
        Activity::class,
        TimeSegment::class,
        ClosingBatch::class,
        ClosingEntry::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timeTrackerDao(): TimeTrackerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN isPaused INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN lastPausedTime INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE sessions ADD COLUMN pausedDuration INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN pauseEvents TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sessions ADD COLUMN discountValue REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN discountPercentage REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN tag TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Clientes: novos campos (arquivamento, moeda, notas)
                db.execSQL("ALTER TABLE clients ADD COLUMN currency TEXT NOT NULL DEFAULT 'BRL'")
                db.execSQL("ALTER TABLE clients ADD COLUMN notes TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE clients ADD COLUMN archivedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE clients ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE clients ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")

                // 2. Projetos
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS projects (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        clientId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        billingMode TEXT NOT NULL DEFAULT 'hourly',
                        hourlyRate REAL DEFAULT NULL,
                        budgetMinutes INTEGER DEFAULT NULL,
                        budgetAmount REAL DEFAULT NULL,
                        status TEXT NOT NULL DEFAULT 'active',
                        archivedAt INTEGER DEFAULT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(clientId) REFERENCES clients(id) ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_projects_clientId ON projects(clientId)")

                // 3. Atividades
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS activities (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        defaultBillable INTEGER NOT NULL DEFAULT 1,
                        archivedAt INTEGER DEFAULT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                val now = System.currentTimeMillis()
                db.execSQL("INSERT INTO activities (name, defaultBillable, createdAt) VALUES ('Desenvolvimento', 1, $now)")
                db.execSQL("INSERT INTO activities (name, defaultBillable, createdAt) VALUES ('Design', 1, $now)")
                db.execSQL("INSERT INTO activities (name, defaultBillable, createdAt) VALUES ('Reunião', 1, $now)")
                db.execSQL("INSERT INTO activities (name, defaultBillable, createdAt) VALUES ('Suporte', 1, $now)")
                db.execSQL("INSERT INTO activities (name, defaultBillable, createdAt) VALUES ('Consultoria', 1, $now)")

                // 4. Sessões: novos campos do WorkSession
                db.execSQL("ALTER TABLE sessions ADD COLUMN projectId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE sessions ADD COLUMN activityId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE sessions ADD COLUMN billable INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE sessions ADD COLUMN appliedRate REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN status TEXT NOT NULL DEFAULT 'completed'")
                db.execSQL("ALTER TABLE sessions ADD COLUMN source TEXT NOT NULL DEFAULT 'timer'")
                db.execSQL("ALTER TABLE sessions ADD COLUMN financialStatus TEXT NOT NULL DEFAULT 'unbilled'")
                db.execSQL("ALTER TABLE sessions ADD COLUMN closingBatchId INTEGER DEFAULT NULL")

                // Preencher appliedRate histórico garantindo que taxas passadas não se percam
                db.execSQL("""
                    UPDATE sessions 
                    SET appliedRate = COALESCE(
                        (SELECT hourlyRate FROM clients WHERE clients.id = sessions.clientId),
                        0.0
                    )
                    WHERE appliedRate = 0.0
                """.trimIndent())

                db.execSQL("UPDATE sessions SET status = 'completed' WHERE endTime IS NOT NULL")
                db.execSQL("UPDATE sessions SET status = CASE WHEN isPaused = 1 THEN 'paused' ELSE 'running' END WHERE endTime IS NULL")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_projectId ON sessions(projectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_closingBatchId ON sessions(closingBatchId)")

                // 5. Segmentos de Tempo
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS time_segments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        startedAt INTEGER NOT NULL,
                        endedAt INTEGER DEFAULT NULL,
                        FOREIGN KEY(sessionId) REFERENCES sessions(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_time_segments_sessionId ON time_segments(sessionId)")

                // 6. Fechamentos Financeiros
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS closing_batches (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        clientId INTEGER NOT NULL,
                        fromDate INTEGER NOT NULL,
                        toDate INTEGER NOT NULL,
                        status TEXT NOT NULL DEFAULT 'invoiced',
                        totalHours REAL NOT NULL DEFAULT 0.0,
                        totalAmount REAL NOT NULL DEFAULT 0.0,
                        sessionCount INTEGER NOT NULL DEFAULT 0,
                        invoiceNumber TEXT DEFAULT NULL,
                        notes TEXT DEFAULT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(clientId) REFERENCES clients(id) ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_closing_batches_clientId ON closing_batches(clientId)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS closing_entries (
                        closingBatchId INTEGER NOT NULL,
                        sessionId INTEGER NOT NULL,
                        PRIMARY KEY(closingBatchId, sessionId),
                        FOREIGN KEY(closingBatchId) REFERENCES closing_batches(id) ON DELETE CASCADE,
                        FOREIGN KEY(sessionId) REFERENCES sessions(id) ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_closing_entries_closingBatchId ON closing_entries(closingBatchId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_closing_entries_sessionId ON closing_entries(sessionId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "time_tracker_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

