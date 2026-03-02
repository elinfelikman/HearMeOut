package com.example.hearmeout

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the transcription history table.
 * Defines the SQL operations for interacting with the Room database.
 */
@Dao
interface TranscriptDao {

    /**
     * Inserts a new conversation record.
     * Marked as 'suspend' to ensure it runs within a Coroutine,
     * preventing UI thread blocking during disk I/O.
     */
    @Insert
    suspend fun insert(transcript: TranscriptEntity)

    /**
     * Retrieves all saved transcripts, ordered by the most recent first.
     * Returns a [Flow], enabling the UI to reactively update
     * whenever the underlying data changes.
     */
    @Query("SELECT * FROM transcripts ORDER BY timestamp DESC")
    fun getAllTranscripts(): Flow<List<TranscriptEntity>>

    /**
     * Clears all records from the transcription table.
     * Typically used for 'Clear History' functionality.
     */
    @Query("DELETE FROM transcripts")
    suspend fun deleteAll()
}