package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcards",
    indices = [
        Index(value = ["category"]),
        Index(value = ["nextReviewDate"])
    ]
)
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val frontText: String,
    val backText: String,
    val category: String, // "General" or "Medical"
    val nextReviewDate: Long = System.currentTimeMillis(),
    val easeFactor: Float = 2.5f,
    val interval: Int = 1
)
