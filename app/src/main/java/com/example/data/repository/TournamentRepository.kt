package com.example.data.repository

import com.example.data.model.DrawMode
import com.example.data.model.Participant
import com.example.data.model.Round
import com.example.data.model.Tournament
import com.example.data.model.TournamentMatch
import kotlinx.coroutines.flow.Flow

/**
 * Capa de repositorio para la gestión del estado del torneo y su cuadro eliminatorio (Winner Bracket).
 * Abstrae las fuentes de datos locales (Room DAOs) y encapsula la lógica de negocio del torneo.
 */
interface TournamentRepository {

    /**
     * Flujo reactivo con todos los torneos registrados ordenados por fecha de creación.
     */
    val allTournaments: Flow<List<Tournament>>

    /**
     * Obtiene un torneo específico de manera reactiva por su identificador.
     */
    fun getTournament(id: String): Flow<Tournament?>

    /**
     * Obtiene los partidos de un torneo ordenados por ronda y posición.
     */
    fun getMatches(tournamentId: String): Flow<List<TournamentMatch>>

    /**
     * Obtiene las fases o rondas del bracket (Octavos, Cuartos, Semifinal, Final).
     */
    fun getRounds(tournamentId: String): Flow<List<Round>>

    /**
     * Obtiene la lista de participantes registrados en un torneo.
     */
    fun getParticipants(tournamentId: String): Flow<List<Participant>>

    /**
     * Crea un nuevo torneo, genera las fases (Rounds) y construye los emparejamientos
     * iniciales de la primera ronda a partir de la lista de participantes, resolviendo BYEs
     * y configurando los enlaces de avance hacia las rondas siguientes.
     *
     * @param name Nombre identificativo del torneo.
     * @param rawParticipantNames Lista de nombres de participantes / equipos.
     * @param drawMode Modalidad de sorteo (RANDOM_ANIMATED, SEEDED o MANUAL).
     * @return ID del torneo recién creado.
     */
    suspend fun createTournament(
        name: String,
        rawParticipantNames: List<String>,
        drawMode: DrawMode
    ): String

    /**
     * Aplica el resultado del sorteo de cruces a los partidos de la primera ronda (Ronda 0).
     */
    suspend fun applyDrawOrder(tournamentId: String, orderedParticipants: List<Participant>)

    /**
     * Registra el marcador o ganador de un partido y ejecuta en tiempo real el avance automático
     * del ganador a la siguiente ronda del bracket.
     */
    suspend fun updateMatchResult(
        matchId: String,
        score1: Int?,
        score2: Int?,
        penalties1: Int? = null,
        penalties2: Int? = null,
        forcedWinnerId: String? = null
    )

    /**
     * Reinicia el resultado de un partido y limpia en cascada las ranuras posteriores del bracket.
     */
    suspend fun resetMatchResult(matchId: String)

    /**
     * Reinicia todo el torneo a su estado inicial.
     */
    suspend fun resetTournament(tournamentId: String)

    /**
     * Elimina el torneo y todos sus datos asociados (partidos, rondas, participantes).
     */
    suspend fun deleteTournament(tournamentId: String)
}
