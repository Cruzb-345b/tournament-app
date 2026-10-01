package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.MatchDao
import com.example.data.dao.ParticipantDao
import com.example.data.dao.RoundDao
import com.example.data.dao.TournamentDao
import com.example.data.entity.MatchEntity
import com.example.data.entity.ParticipantEntity
import com.example.data.entity.RoundEntity
import com.example.data.entity.TournamentEntity

@Database(
    entities = [
        TournamentEntity::class,
        ParticipantEntity::class,
        RoundEntity::class,
        MatchEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TournamentDatabase : RoomDatabase() {
    abstract fun tournamentDao(): TournamentDao
    abstract fun participantDao(): ParticipantDao
    abstract fun roundDao(): RoundDao
    abstract fun matchDao(): MatchDao

    companion object {
        @Volatile
        private var INSTANCE: TournamentDatabase? = null

        fun getDatabase(context: Context): TournamentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TournamentDatabase::class.java,
                    "tournament_pro_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
