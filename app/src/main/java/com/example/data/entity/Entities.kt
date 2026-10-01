package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad principal que representa un Torneo en la base de datos.
 */
@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val status: String,
    val drawMode: String,
    val totalParticipants: Int,
    val createdAt: Long,
    val championId: String? = null,
    val championName: String? = null,
    val runnerUpId: String? = null,
    val runnerUpName: String? = null,
    val thirdPlaceId: String? = null,
    val thirdPlaceName: String? = null
)

/**
 * Entidad que representa a un Participante (jugador, equipo o pareja) registrado en el torneo.
 */
@Entity(
    tableName = "participants",
    indices = [Index(value = ["tournamentId"])]
)
data class ParticipantEntity(
    @PrimaryKey val id: String,
    val tournamentId: String,
    val name: String,
    val seed: Int,
    val colorHex: String
)

/**
 * Entidad que representa una Ronda o Fase del Winner Bracket (ej. Octavos, Cuartos, Semifinal, Final).
 */
@Entity(
    tableName = "tournament_rounds",
    indices = [Index(value = ["tournamentId"])]
)
data class RoundEntity(
    @PrimaryKey val id: String,
    val tournamentId: String,
    val roundIndex: Int,            // 0 = Primera ronda, 1 = Segunda, etc.
    val name: String,                  // "Octavos de Final", "Cuartos", "Semifinal", "Gran Final"
    val totalMatches: Int,             // Cantidad de partidos en esta fase
    val isCompleted: Boolean = false   // Indica si todos los partidos de la ronda han concluido
)

/**
 * Entidad que representa un Partido (Match) del cuadro eliminatorio.
 * Contiene referencias al torneo, a la ronda, a los participantes y al match siguiente en el bracket.
 */
@Entity(
    tableName = "tournament_matches",
    indices = [
        Index(value = ["tournamentId"]),
        Index(value = ["roundId"])
    ]
)
data class MatchEntity(
    @PrimaryKey val id: String,
    val tournamentId: String,
    val roundId: String? = null,       // Relación con RoundEntity
    val roundIndex: Int,               // Índice de la ronda (0, 1, 2...)
    val matchIndexInRound: Int,        // Posición del partido dentro de la ronda
    val totalRounds: Int,              // Total de rondas del torneo
    val roundName: String,             // Nombre legible de la ronda
    val p1Id: String? = null,          // ID del participante 1
    val p1Name: String? = null,        // Nombre del participante 1
    val p1Seed: Int? = null,           // Siembra / cabeza de serie participante 1
    val p1Color: String? = null,       // Color distintivo participante 1
    val p2Id: String? = null,          // ID del participante 2
    val p2Name: String? = null,        // Nombre del participante 2
    val p2Seed: Int? = null,           // Siembra / cabeza de serie participante 2
    val p2Color: String? = null,       // Color distintivo participante 2
    val score1: Int? = null,           // Puntuación / goles participante 1
    val score2: Int? = null,           // Puntuación / goles participante 2
    val penalties1: Int? = null,       // Desempate penaltis participante 1
    val penalties2: Int? = null,       // Desempate penaltis participante 2
    val winnerId: String? = null,      // ID del ganador que avanza
    val winnerName: String? = null,    // Nombre del ganador que avanza
    val isBye: Boolean = false,        // Clasificación directa sin rival
    val isCompleted: Boolean = false,  // Partido finalizado
    val nextMatchId: String? = null,   // Partido destino en la siguiente ronda del bracket
    val nextMatchSlot: Int = 1         // Ranura de destino (1 = arriba, 2 = abajo)
)
