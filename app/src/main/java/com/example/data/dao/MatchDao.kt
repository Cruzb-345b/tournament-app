package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.MatchEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para las operaciones de la entidad Match (Partidos del Bracket).
 */
@Dao
interface MatchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Query("SELECT * FROM tournament_matches WHERE tournamentId = :tournamentId ORDER BY roundIndex ASC, matchIndexInRound ASC")
    fun getMatches(tournamentId: String): Flow<List<MatchEntity>>

    @Query("SELECT * FROM tournament_matches WHERE tournamentId = :tournamentId ORDER BY roundIndex ASC, matchIndexInRound ASC")
    suspend fun getMatchesOnce(tournamentId: String): List<MatchEntity>

    @Query("SELECT * FROM tournament_matches WHERE roundId = :roundId ORDER BY matchIndexInRound ASC")
    fun getMatchesByRound(roundId: String): Flow<List<MatchEntity>>

    @Query("SELECT * FROM tournament_matches WHERE tournamentId = :tournamentId AND roundIndex = :roundIndex ORDER BY matchIndexInRound ASC")
    suspend fun getMatchesByRoundIndex(tournamentId: String, roundIndex: Int): List<MatchEntity>

    @Query("SELECT * FROM tournament_matches WHERE id = :matchId LIMIT 1")
    suspend fun getMatchById(matchId: String): MatchEntity?

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Update
    suspend fun updateMatches(matches: List<MatchEntity>)

    @Query("DELETE FROM tournament_matches WHERE id = :matchId")
    suspend fun deleteMatchById(matchId: String)

    @Query("DELETE FROM tournament_matches WHERE tournamentId = :tournamentId")
    suspend fun deleteMatchesByTournament(tournamentId: String)
}
