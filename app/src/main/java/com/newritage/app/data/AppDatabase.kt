package com.newritage.app.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Session::class, SensorReading::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN sessionIndex INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE sessions ADD COLUMN hasThread INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN startTime TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sessions ADD COLUMN endTime TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sessions ADD COLUMN threadColorName TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN aiFeedback TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN medianPressure REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN thumbAvg REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN thumbMin REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN thumbMax REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN thumbMedian REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN imAvg REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN imMin REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN imMax REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN imMedian REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN palmAvg REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN palmMin REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN palmMax REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN palmMedian REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN vibrationCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sensor_readings` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `thumb` REAL NOT NULL,
                        `indexMiddle` REAL NOT NULL,
                        `palm` REAL NOT NULL,
                        `overall` REAL NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "newritage_db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
