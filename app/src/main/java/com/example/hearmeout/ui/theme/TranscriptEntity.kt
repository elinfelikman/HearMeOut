package com.example.hearmeout

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * TranscriptEntity: Represents a single conversation record in the Room Database.
 * Each instance of this class corresponds to a row in the "transcripts" table.
 * */
@Entity(tableName = "transcripts")
data class TranscriptEntity(
    /**
     * Unique identifier for each transcript.
     * [autoGenerate] ensures that Room handles ID creation automatically.
     */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /**
     * The title of the conversation (e.g., "Doctor's Visit" or "Call with Mom").
     */
    val title: String,

    /**
     * The full text content of the transcribed conversation.
     */
    val content: String,

    /**
     * System timestamp (in milliseconds) used for chronological sorting in the UI.
     */
    val timestamp: Long
)