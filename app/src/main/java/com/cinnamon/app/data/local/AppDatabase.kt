package com.cinnamon.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LexiconEntry::class,
        Morpheme::class,
        Abbreviation::class,
        PhraseEntry::class,
        Confusable::class,
        PracticeSentence::class,
        Flashcard::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lexiconDao(): LexiconDao
    abstract fun learnDao(): LearnDao
    abstract fun flashcardDao(): FlashcardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cinnamon.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
