package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ParticipantEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para las operaciones de la entidad Participant.
 */
@Dao
interface ParticipantDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ParticipantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ParticipantEntity>)

    @Query("SELECT * FROM participants WHERE tournamentId = :tournamentId ORDER BY seed ASC")
    fun getParticipants(tournamentId: String): Flow<List<ParticipantEntity>>

    @Query("SELECT * FROM participants WHERE tournamentId = :tournamentId ORDER BY seed ASC")
    suspend fun getParticipantsOnce(tournamentId: String): List<ParticipantEntity>

    @Query("SELECT * FROM participants WHERE id = :id LIMIT 1")
    suspend fun getParticipantById(id: String): ParticipantEntity?

    @Update
    suspend fun updateParticipant(participant: ParticipantEntity)

    @Query("DELETE FROM participants WHERE id = :id")
    suspend fun deleteParticipantById(id: String)

    @Query("DELETE FROM participants WHERE tournamentId = :tournamentId")
    suspend fun deleteParticipantsByTournament(tournamentId: String)
}
