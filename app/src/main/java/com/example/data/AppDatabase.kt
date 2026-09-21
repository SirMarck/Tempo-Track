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
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timeTrackerDao(): TimeTrackerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun addColumnIfNotExists(db: SupportSQLiteDatabase, tableName: String, columnName: String, columnDef: String) {
            val cursor = db.query("PRAGMA table_info(`$tableName`)")
            var exists = false
            cursor.use {
                val nameIndex = it.getColumnIndex("name")
                if (nameIndex >= 0) {
                    while (it.moveToNext()) {
                        if (it.getString(nameIndex).equals(columnName, ignoreCase = true)) {
                            exists = true
                            break
                        }
                    }
                }
            }
            if (!exists) {
                db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDef")
            }
        }

        private fun migrateV4ToV6(db: SupportSQLiteDatabase) {
            // 1. Clientes: novos campos (arquivamento, moeda, notas)
            addColumnIfNotExists(db, "clients", "currency", "TEXT NOT NULL DEFAULT 'BRL'")
            addColumnIfNotExists(db, "clients", "notes", "TEXT DEFAULT NULL")
            addColumnIfNotExists(db, "clients", "archivedAt", "INTEGER DEFAULT NULL")
            addColumnIfNotExists(db, "clients", "createdAt", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "clients", "updatedAt", "INTEGER NOT NULL DEFAULT 0")

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

            try {
                val countCursor = db.query("SELECT COUNT(*) FROM activities")
                var hasActivities = false
                countCursor.use {
                    if (it.moveToFirst() && it.getInt(0) > 0) {
                        hasActivities = true
                    }
                }
                if (!hasActivities) {
                    val now = System.currentTimeMillis()
                    db.execSQL("INSERT INTO activities (name, defaultBillable, createdAt) VALUES ('Desenvolvimento', 1, $now)")
                    db.execSQL("INSERT INTO activities (name, defaultBillable, createdAt) VALUES ('Design', 1, $now)")
                    db.execSQL("INSERT INTO activities (name, defaultBillable, createdAt) VALUES ('Reunião', 1, $now)")
                    db.execSQL("INSERT INTO activities (name, defaultBillable, createdAt) VALUES ('Suporte', 1, $now)")
                    db.execSQL("INSERT INTO activities (name, defaultBillable, createdAt) VALUES ('Consultoria', 1, $now)")
                }
            } catch (ignored: Exception) {}

            // 4. Sessões: novos campos do WorkSession
            addColumnIfNotExists(db, "sessions", "projectId", "INTEGER DEFAULT NULL")
            addColumnIfNotExists(db, "sessions", "activityId", "INTEGER DEFAULT NULL")
            addColumnIfNotExists(db, "sessions", "billable", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfNotExists(db, "sessions", "appliedRate", "REAL NOT NULL DEFAULT 0.0")
            addColumnIfNotExists(db, "sessions", "status", "TEXT NOT NULL DEFAULT 'completed'")
            addColumnIfNotExists(db, "sessions", "source", "TEXT NOT NULL DEFAULT 'timer'")
            addColumnIfNotExists(db, "sessions", "financialStatus", "TEXT NOT NULL DEFAULT 'unbilled'")
            addColumnIfNotExists(db, "sessions", "closingBatchId", "INTEGER DEFAULT NULL")

            // Preencher appliedRate histórico garantindo que taxas passadas não se percam
            try {
                db.execSQL("""
                    UPDATE sessions 
                    SET appliedRate = COALESCE(
                        (SELECT hourlyRate FROM clients WHERE clients.id = sessions.clientId),
                        0.0
                    )
                    WHERE appliedRate = 0.0
                """.trimIndent())
            } catch (ignored: Exception) {}

            try {
                db.execSQL("UPDATE sessions SET status = 'completed' WHERE endTime IS NOT NULL")
                db.execSQL("UPDATE sessions SET status = CASE WHEN isPaused = 1 THEN 'paused' ELSE 'running' END WHERE endTime IS NULL")
            } catch (ignored: Exception) {}

            db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_clientId ON sessions(clientId)")
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

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "sessions", "isPaused", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfNotExists(db, "sessions", "lastPausedTime", "INTEGER DEFAULT NULL")
                addColumnIfNotExists(db, "sessions", "pausedDuration", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "sessions", "pauseEvents", "TEXT NOT NULL DEFAULT ''")
                addColumnIfNotExists(db, "sessions", "discountValue", "REAL NOT NULL DEFAULT 0.0")
                addColumnIfNotExists(db, "sessions", "discountPercentage", "REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "sessions", "tag", "TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateV4ToV6(db)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateV4ToV6(db)
            }
        }

        val MIGRATION_4_6 = object : Migration(4, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateV4ToV6(db)
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "time_tracker_database"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_4_6)
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = buildDatabase(context)
                try {
                    // Force open helper to validate migration synchronously
                    db.openHelper.writableDatabase
                    INSTANCE = db
                    db
                } catch (e: Throwable) {
                    android.util.Log.e("AppDatabase", "Migration/DB init failed. Recreating clean database...", e)
                    try {
                        db.close()
                    } catch (ignored: Exception) {}
                    try {
                        context.deleteDatabase("time_tracker_database")
                    } catch (ignored: Exception) {}
                    val fallbackDb = buildDatabase(context)
                    try {
                        fallbackDb.openHelper.writableDatabase
                    } catch (ignored: Exception) {}
                    INSTANCE = fallbackDb
                    fallbackDb
                }
            }
        }
    }
}
