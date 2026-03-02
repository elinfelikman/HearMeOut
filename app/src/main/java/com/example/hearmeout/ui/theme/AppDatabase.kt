package com.example.hearmeout

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Main Database class for the application using Room Persistence Library.
 * [entities] defines the tables in the database (TranscriptEntity in this case).
 * [version] must be incremented whenever the schema changes.
 */
@Database(entities = [TranscriptEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Connects the database to the Data Access Object (DAO).
     */
    abstract fun transcriptDao(): TranscriptDao

    companion object {
        /**
         * @Volatile ensures that the value of INSTANCE is always up-to-date
         * and the same to all execution threads.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the Singleton instance of the database.
         * Uses a thread-safe synchronized block to prevent multiple instances
         * from being created simultaneously.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hearmeout_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}