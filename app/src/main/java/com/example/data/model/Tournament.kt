package com.example.data.model

enum class TournamentStatus {
    DRAFT_DRAW,
    IN_PROGRESS,
    COMPLETED
}

enum class DrawMode {
    RANDOM_ANIMATED, // Sorteo aleatorio con bombo interactivo
    SEEDED,          // Cabezas de serie (1 vs 8, 4 vs 5, etc.)
    MANUAL           // Orden secuencial ingresado
}

/**
 * Modelo de dominio para un Participante
 */
data class Participant(
    val id: String,
    val name: String,
    val seed: Int = 0,
    val colorHex: String = "#6366F1"
)

/**
 * Modelo de dominio para una Ronda del torneo
 */
data class Round(
    val id: String,
    val tournamentId: String,
    val roundIndex: Int,
    val name: String,
    val totalMatches: Int,
    val isCompleted: Boolean = false
)

/**
 * Modelo de dominio para un Partido (Match) del Winner Bracket
 */
data class TournamentMatch(
    val id: String,
    val tournamentId: String,
    val roundId: String? = null,
    val roundIndex: Int,            // 0 = first round, 1, 2...
    val matchIndexInRound: Int,     // 0, 1, 2, ...
    val totalRounds: Int,
    val roundName: String,          // "Octavos de Final", "Cuartos", "Semifinal", "Final"
    val participant1: Participant?,
    val participant2: Participant?,
    val score1: Int? = null,
    val score2: Int? = null,
    val penalties1: Int? = null,
    val penalties2: Int? = null,
    val winner: Participant? = null,
    val isBye: Boolean = false,
    val isCompleted: Boolean = false,
    val nextMatchId: String? = null,
    val nextMatchSlot: Int = 1      // 1 = top slot, 2 = bottom slot
)

/**
 * Modelo de dominio para el Torneo
 */
data class Tournament(
    val id: String,
    val name: String,
    val status: TournamentStatus,
    val drawMode: DrawMode,
    val totalParticipants: Int,
    val createdAt: Long,
    val champion: Participant? = null,
    val runnerUp: Participant? = null,
    val thirdPlace: Participant? = null,
    val rounds: List<Round> = emptyList()
)

data class TournamentStats(
    val totalMatches: Int,
    val completedMatches: Int,
    val totalGoalsOrPoints: Int,
    val progressPercentage: Float,
    val highestScoringMatch: String? = null
)
