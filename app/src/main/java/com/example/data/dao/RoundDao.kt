package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.RoundEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para las operaciones de la entidad Round (Fases del Bracket).
 */
@Dao
interface RoundDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRound(round: RoundEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRounds(rounds: List<RoundEntity>)

    @Query("SELECT * FROM tournament_rounds WHERE tournamentId = :tournamentId ORDER BY roundIndex ASC")
    fun getRounds(tournamentId: String): Flow<List<RoundEntity>>

    @Query("SELECT * FROM tournament_rounds WHERE tournamentId = :tournamentId ORDER BY roundIndex ASC")
    suspend fun getRoundsOnce(tournamentId: String): List<RoundEntity>

    @Query("SELECT * FROM tournament_rounds WHERE id = :roundId LIMIT 1")
    suspend fun getRoundById(roundId: String): RoundEntity?

    @Query("SELECT * FROM tournament_rounds WHERE tournamentId = :tournamentId AND roundIndex = :roundIndex LIMIT 1")
    suspend fun getRoundByIndex(tournamentId: String, roundIndex: Int): RoundEntity?

    @Update
    suspend fun updateRound(round: RoundEntity)

    @Query("UPDATE tournament_rounds SET isCompleted = :isCompleted WHERE id = :roundId")
    suspend fun updateRoundCompletionStatus(roundId: String, isCompleted: Boolean)

    @Query("DELETE FROM tournament_rounds WHERE id = :roundId")
    suspend fun deleteRoundById(roundId: String)

    @Query("DELETE FROM tournament_rounds WHERE tournamentId = :tournamentId")
    suspend fun deleteRoundsByTournament(tournamentId: String)
}
