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
        Flashcard::class,
        GamificationEventEntity::class,
        RewardTransactionEntity::class,
        RewardSummaryEntity::class,
        RewardBalanceEntity::class,
        QuestInstanceEntity::class,
        JourneyInstanceEntity::class,
        JourneyStageProgressEntity::class,
        CampaignRouteChoiceEntity::class,
        AchievementUnlockEntity::class,
        RewardPresentationReceiptEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lexiconDao(): LexiconDao
    abstract fun learnDao(): LearnDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun gamificationDao(): GamificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cinnamon.db"
                )
                    .addMigrations(
                        AppDatabaseMigrations.MIGRATION_1_2,
                        AppDatabaseMigrations.MIGRATION_2_3,
                        AppDatabaseMigrations.MIGRATION_3_4
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
