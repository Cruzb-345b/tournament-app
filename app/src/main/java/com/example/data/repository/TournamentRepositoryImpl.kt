package com.example.data.repository

import com.example.data.TournamentDatabase
import com.example.data.dao.MatchDao
import com.example.data.dao.ParticipantDao
import com.example.data.dao.RoundDao
import com.example.data.dao.TournamentDao
import com.example.data.entity.MatchEntity
import com.example.data.entity.ParticipantEntity
import com.example.data.entity.RoundEntity
import com.example.data.entity.TournamentEntity
import com.example.data.model.DrawMode
import com.example.data.model.Participant
import com.example.data.model.Round
import com.example.data.model.Tournament
import com.example.data.model.TournamentMatch
import com.example.data.model.TournamentStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import kotlin.math.log2
import kotlin.math.pow

/**
 * Implementación de la capa de Repositorio que interactúa con los DAOs de Room
 * para administrar el ciclo de vida del torneo, la generación algorítmica de los cruces
 * de la primera ronda y la progresión en tiempo real del Winner Bracket.
 */
class TournamentRepositoryImpl(
    private val tournamentDao: TournamentDao,
    private val participantDao: ParticipantDao,
    private val roundDao: RoundDao,
    private val matchDao: MatchDao
) : TournamentRepository {

    constructor(database: TournamentDatabase) : this(
        tournamentDao = database.tournamentDao(),
        participantDao = database.participantDao(),
        roundDao = database.roundDao(),
        matchDao = database.matchDao()
    )

    override val allTournaments: Flow<List<Tournament>> = tournamentDao.getAllTournaments().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getTournament(id: String): Flow<Tournament?> {
        return tournamentDao.getTournamentById(id).map { it?.toDomain() }
    }

    override fun getMatches(tournamentId: String): Flow<List<TournamentMatch>> {
        return matchDao.getMatches(tournamentId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRounds(tournamentId: String): Flow<List<Round>> {
        return roundDao.getRounds(tournamentId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getParticipants(tournamentId: String): Flow<List<Participant>> {
        return participantDao.getParticipants(tournamentId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Crea el torneo y genera los cruces iniciales a partir de la lista de participantes.
     */
    override suspend fun createTournament(
        name: String,
        rawParticipantNames: List<String>,
        drawMode: DrawMode
    ): String {
        val tournamentId = UUID.randomUUID().toString()
        val cleanedNames = rawParticipantNames
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val palette = listOf(
            "#4F46E5", "#06B6D4", "#10B981", "#F59E0B",
            "#EC4899", "#8B5CF6", "#14B8A6", "#EF4444",
            "#3B82F6", "#6366F1", "#84CC16", "#F97316"
        )

        val participants = cleanedNames.mapIndexed { index, pName ->
            Participant(
                id = UUID.randomUUID().toString(),
                name = pName,
                seed = index + 1,
                colorHex = palette[index % palette.size]
            )
        }

        val initialStatus = if (drawMode == DrawMode.RANDOM_ANIMATED) {
            TournamentStatus.DRAFT_DRAW
        } else {
            TournamentStatus.IN_PROGRESS
        }

        val tournamentEntity = TournamentEntity(
            id = tournamentId,
            name = name.ifBlank { "Torneo Eliminatorio" },
            status = initialStatus.name,
            drawMode = drawMode.name,
            totalParticipants = participants.size,
            createdAt = System.currentTimeMillis()
        )

        tournamentDao.insertTournament(tournamentEntity)

        val participantEntities = participants.map { p ->
            ParticipantEntity(
                id = p.id,
                tournamentId = tournamentId,
                name = p.name,
                seed = p.seed,
                colorHex = p.colorHex
            )
        }
        participantDao.insertParticipants(participantEntities)

        // Generar estructura del bracket (Rondas y Partidos iniciales)
        val (rounds, matches) = generateBracketStructure(
            tournamentId = tournamentId,
            participants = participants,
            drawMode = drawMode
        )
        roundDao.insertRounds(rounds)
        matchDao.insertMatches(matches)

        return tournamentId
    }

    /**
     * Aplica el resultado del sorteo aleatorio a los partidos de la Ronda 0
     */
    override suspend fun applyDrawOrder(tournamentId: String, orderedParticipants: List<Participant>) {
        val matches = matchDao.getMatchesOnce(tournamentId).toMutableList()
        val round0Matches = matches.filter { it.roundIndex == 0 }.sortedBy { it.matchIndexInRound }

        val updatedMatches = mutableListOf<MatchEntity>()

        var pIndex = 0
        for (m in round0Matches) {
            val p1 = orderedParticipants.getOrNull(pIndex)
            val p2 = orderedParticipants.getOrNull(pIndex + 1)
            pIndex += 2

            val isBye = (p1 != null && p2 == null) || (p1 == null && p2 != null)
            val byeWinner = if (isBye) (p1 ?: p2) else null

            val updatedMatch = m.copy(
                p1Id = p1?.id,
                p1Name = p1?.name,
                p1Seed = p1?.seed,
                p1Color = p1?.colorHex,
                p2Id = p2?.id,
                p2Name = p2?.name,
                p2Seed = p2?.seed,
                p2Color = p2?.colorHex,
                isBye = isBye,
                isCompleted = isBye,
                winnerId = byeWinner?.id,
                winnerName = byeWinner?.name
            )
            updatedMatches.add(updatedMatch)
        }

        matchDao.updateMatches(updatedMatches)

        // Avance automático a Ronda 1 para partidos que tengan BYE
        for (m in updatedMatches) {
            if (m.isBye && m.winnerId != null && m.nextMatchId != null) {
                advanceWinnerToNextMatch(m.nextMatchId, m.nextMatchSlot, m.winnerId, m.winnerName ?: "")
            }
        }

        val t = tournamentDao.getTournamentByIdOnce(tournamentId)
        if (t != null) {
            tournamentDao.updateTournament(t.copy(status = TournamentStatus.IN_PROGRESS.name))
        }
    }

    /**
     * Actualiza el marcador o declara un ganador en tiempo real, propagando el resultado al siguiente nodo.
     */
    override suspend fun updateMatchResult(
        matchId: String,
        score1: Int?,
        score2: Int?,
        penalties1: Int?,
        penalties2: Int?,
        forcedWinnerId: String?
    ) {
        val match = matchDao.getMatchById(matchId) ?: return

        val winnerId: String?
        val winnerName: String?

        if (forcedWinnerId != null) {
            winnerId = forcedWinnerId
            winnerName = if (forcedWinnerId == match.p1Id) match.p1Name else match.p2Name
        } else if (score1 != null && score2 != null) {
            if (score1 > score2) {
                winnerId = match.p1Id
                winnerName = match.p1Name
            } else if (score2 > score1) {
                winnerId = match.p2Id
                winnerName = match.p2Name
            } else if (penalties1 != null && penalties2 != null && penalties1 != penalties2) {
                if (penalties1 > penalties2) {
                    winnerId = match.p1Id
                    winnerName = match.p1Name
                } else {
                    winnerId = match.p2Id
                    winnerName = match.p2Name
                }
            } else {
                winnerId = null
                winnerName = null
            }
        } else {
            winnerId = null
            winnerName = null
        }

        val previousWinnerId = match.winnerId
        val isCompleted = winnerId != null

        val updatedMatch = match.copy(
            score1 = score1,
            score2 = score2,
            penalties1 = penalties1,
            penalties2 = penalties2,
            winnerId = winnerId,
            winnerName = winnerName,
            isCompleted = isCompleted
        )

        matchDao.updateMatch(updatedMatch)

        // Avance automático en el bracket hacia el siguiente partido
        if (match.nextMatchId != null) {
            if (winnerId != null) {
                advanceWinnerToNextMatch(
                    nextMatchId = match.nextMatchId,
                    slot = match.nextMatchSlot,
                    winnerId = winnerId,
                    winnerName = winnerName ?: ""
                )
            } else if (previousWinnerId != null) {
                clearDownstreamMatchSlot(match.nextMatchId, match.nextMatchSlot)
            }
        } else if (match.roundIndex == match.totalRounds - 1 && isCompleted) {
            // Se completó la Gran Final -> Declarar Campeón y Subcampeón
            val runnerUpId = if (winnerId == match.p1Id) match.p2Id else match.p1Id
            val runnerUpName = if (winnerId == match.p1Id) match.p2Name else match.p1Name
            val tournament = tournamentDao.getTournamentByIdOnce(match.tournamentId)
            if (tournament != null) {
                tournamentDao.updateTournament(
                    tournament.copy(
                        status = TournamentStatus.COMPLETED.name,
                        championId = winnerId,
                        championName = winnerName,
                        runnerUpId = runnerUpId,
                        runnerUpName = runnerUpName
                    )
                )
            }
        }

        checkAndUpdateRoundStatus(match.tournamentId, match.roundIndex)
    }

    override suspend fun resetMatchResult(matchId: String) {
        val match = matchDao.getMatchById(matchId) ?: return
        val updated = match.copy(
            score1 = null,
            score2 = null,
            penalties1 = null,
            penalties2 = null,
            winnerId = null,
            winnerName = null,
            isCompleted = false
        )
        matchDao.updateMatch(updated)

        if (match.nextMatchId != null) {
            clearDownstreamMatchSlot(match.nextMatchId, match.nextMatchSlot)
        } else if (match.roundIndex == match.totalRounds - 1) {
            val t = tournamentDao.getTournamentByIdOnce(match.tournamentId)
            if (t != null) {
                tournamentDao.updateTournament(
                    t.copy(
                        status = TournamentStatus.IN_PROGRESS.name,
                        championId = null,
                        championName = null,
                        runnerUpId = null,
                        runnerUpName = null
                    )
                )
            }
        }

        checkAndUpdateRoundStatus(match.tournamentId, match.roundIndex)
    }

    override suspend fun resetTournament(tournamentId: String) {
        val participants = participantDao.getParticipantsOnce(tournamentId).map { it.toDomain() }
        val t = tournamentDao.getTournamentByIdOnce(tournamentId) ?: return

        matchDao.deleteMatchesByTournament(tournamentId)
        roundDao.deleteRoundsByTournament(tournamentId)

        val (rounds, newMatches) = generateBracketStructure(
            tournamentId = tournamentId,
            participants = participants,
            drawMode = DrawMode.valueOf(t.drawMode)
        )
        roundDao.insertRounds(rounds)
        matchDao.insertMatches(newMatches)

        val initialStatus = if (DrawMode.valueOf(t.drawMode) == DrawMode.RANDOM_ANIMATED) {
            TournamentStatus.DRAFT_DRAW
        } else {
            TournamentStatus.IN_PROGRESS
        }

        tournamentDao.updateTournament(
            t.copy(
                status = initialStatus.name,
                championId = null,
                championName = null,
                runnerUpId = null,
                runnerUpName = null,
                thirdPlaceId = null,
                thirdPlaceName = null
            )
        )
    }

    override suspend fun deleteTournament(tournamentId: String) {
        matchDao.deleteMatchesByTournament(tournamentId)
        roundDao.deleteRoundsByTournament(tournamentId)
        participantDao.deleteParticipantsByTournament(tournamentId)
        tournamentDao.deleteTournament(tournamentId)
    }

    /**
     * Lógica de generación de rondas y emparejamientos del cuadro eliminatorio
     */
    private fun generateBracketStructure(
        tournamentId: String,
        participants: List<Participant>,
        drawMode: DrawMode
    ): Pair<List<RoundEntity>, List<MatchEntity>> {
        val count = participants.size.coerceAtLeast(2)
        val bracketSize = calculateNextPowerOfTwo(count)
        val totalRounds = (log2(bracketSize.toDouble())).toInt()

        val orderedParticipants: List<Participant?> = when (drawMode) {
            DrawMode.SEEDED -> generateSeededBracketSlots(participants, bracketSize)
            DrawMode.MANUAL -> generateSequentialSlots(participants, bracketSize)
            DrawMode.RANDOM_ANIMATED -> List(bracketSize) { null }
        }

        val roundEntities = mutableListOf<RoundEntity>()
        val roundIdMap = mutableMapOf<Int, String>()
        val matchIds = mutableMapOf<Pair<Int, Int>, String>()

        for (r in 0 until totalRounds) {
            val roundId = UUID.randomUUID().toString()
            roundIdMap[r] = roundId
            val matchesInRound = bracketSize / (2.0.pow(r + 1).toInt())
            val roundName = getRoundName(roundIndex = r, totalRounds = totalRounds)

            roundEntities.add(
                RoundEntity(
                    id = roundId,
                    tournamentId = tournamentId,
                    roundIndex = r,
                    name = roundName,
                    totalMatches = matchesInRound,
                    isCompleted = false
                )
            )

            for (m in 0 until matchesInRound) {
                matchIds[Pair(r, m)] = UUID.randomUUID().toString()
            }
        }

        val allMatches = mutableListOf<MatchEntity>()

        for (r in 0 until totalRounds) {
            val matchesInRound = bracketSize / (2.0.pow(r + 1).toInt())
            val roundName = getRoundName(roundIndex = r, totalRounds = totalRounds)
            val currentRoundId = roundIdMap[r]!!

            for (m in 0 until matchesInRound) {
                val matchId = matchIds[Pair(r, m)]!!
                val nextMatchId = if (r < totalRounds - 1) matchIds[Pair(r + 1, m / 2)] else null
                val nextMatchSlot = if (m % 2 == 0) 1 else 2

                if (r == 0) {
                    val p1 = orderedParticipants.getOrNull(m * 2)
                    val p2 = orderedParticipants.getOrNull(m * 2 + 1)
                    val isBye = (p1 != null && p2 == null) || (p1 == null && p2 != null)
                    val byeWinner = if (isBye) (p1 ?: p2) else null

                    val matchEntity = MatchEntity(
                        id = matchId,
                        tournamentId = tournamentId,
                        roundId = currentRoundId,
                        roundIndex = r,
                        matchIndexInRound = m,
                        totalRounds = totalRounds,
                        roundName = roundName,
                        p1Id = p1?.id,
                        p1Name = p1?.name,
                        p1Seed = p1?.seed,
                        p1Color = p1?.colorHex,
                        p2Id = p2?.id,
                        p2Name = p2?.name,
                        p2Seed = p2?.seed,
                        p2Color = p2?.colorHex,
                        isBye = isBye,
                        isCompleted = isBye,
                        winnerId = byeWinner?.id,
                        winnerName = byeWinner?.name,
                        nextMatchId = nextMatchId,
                        nextMatchSlot = nextMatchSlot
                    )
                    allMatches.add(matchEntity)
                } else {
                    val matchEntity = MatchEntity(
                        id = matchId,
                        tournamentId = tournamentId,
                        roundId = currentRoundId,
                        roundIndex = r,
                        matchIndexInRound = m,
                        totalRounds = totalRounds,
                        roundName = roundName,
                        p1Id = null,
                        p1Name = null,
                        p2Id = null,
                        p2Name = null,
                        nextMatchId = nextMatchId,
                        nextMatchSlot = nextMatchSlot
                    )
                    allMatches.add(matchEntity)
                }
            }
        }

        // Avance automático en la estructura de los partidos que son BYE
        val updatedMatches = allMatches.toMutableList()
        val r0Matches = updatedMatches.filter { it.roundIndex == 0 }
        for (m0 in r0Matches) {
            if (m0.isBye && m0.winnerId != null && m0.nextMatchId != null) {
                val nextIndex = updatedMatches.indexOfFirst { it.id == m0.nextMatchId }
                if (nextIndex != -1) {
                    val nextMatch = updatedMatches[nextIndex]
                    val updatedNext = if (m0.nextMatchSlot == 1) {
                        nextMatch.copy(
                            p1Id = m0.winnerId,
                            p1Name = m0.winnerName,
                            p1Seed = m0.p1Seed ?: m0.p2Seed,
                            p1Color = m0.p1Color ?: m0.p2Color
                        )
                    } else {
                        nextMatch.copy(
                            p2Id = m0.winnerId,
                            p2Name = m0.winnerName,
                            p2Seed = m0.p1Seed ?: m0.p2Seed,
                            p2Color = m0.p1Color ?: m0.p2Color
                        )
                    }
                    updatedMatches[nextIndex] = updatedNext
                }
            }
        }

        return Pair(roundEntities, updatedMatches)
    }

    private suspend fun checkAndUpdateRoundStatus(tournamentId: String, roundIndex: Int) {
        val allMatchesInRound = matchDao.getMatchesOnce(tournamentId).filter { it.roundIndex == roundIndex }
        val isRoundDone = allMatchesInRound.isNotEmpty() && allMatchesInRound.all { it.isCompleted }
        val rounds = roundDao.getRoundsOnce(tournamentId)
        val roundEntity = rounds.find { it.roundIndex == roundIndex }
        if (roundEntity != null && roundEntity.isCompleted != isRoundDone) {
            roundDao.updateRound(roundEntity.copy(isCompleted = isRoundDone))
        }
    }

    private suspend fun advanceWinnerToNextMatch(
        nextMatchId: String,
        slot: Int,
        winnerId: String,
        winnerName: String
    ) {
        val nextMatch = matchDao.getMatchById(nextMatchId) ?: return

        val participant = participantDao.getParticipantsOnce(nextMatch.tournamentId).find { it.id == winnerId }
        val pSeed = participant?.seed
        val pColor = participant?.colorHex

        val oldWinnerInThisNextMatch = nextMatch.winnerId

        val updated = if (slot == 1) {
            nextMatch.copy(
                p1Id = winnerId,
                p1Name = winnerName,
                p1Seed = pSeed,
                p1Color = pColor
            )
        } else {
            nextMatch.copy(
                p2Id = winnerId,
                p2Name = winnerName,
                p2Seed = pSeed,
                p2Color = pColor
            )
        }

        if (oldWinnerInThisNextMatch != null && oldWinnerInThisNextMatch != winnerId) {
            val cleared = updated.copy(
                score1 = null,
                score2 = null,
                penalties1 = null,
                penalties2 = null,
                winnerId = null,
                winnerName = null,
                isCompleted = false
            )
            matchDao.updateMatch(cleared)
            if (cleared.nextMatchId != null) {
                clearDownstreamMatchSlot(cleared.nextMatchId, cleared.nextMatchSlot)
            }
        } else {
            matchDao.updateMatch(updated)
        }
    }

    private suspend fun clearDownstreamMatchSlot(matchId: String, slot: Int) {
        val match = matchDao.getMatchById(matchId) ?: return
        val updated = if (slot == 1) {
            match.copy(
                p1Id = null,
                p1Name = null,
                p1Seed = null,
                p1Color = null,
                score1 = null,
                score2 = null,
                penalties1 = null,
                penalties2 = null,
                winnerId = null,
                winnerName = null,
                isCompleted = false
            )
        } else {
            match.copy(
                p2Id = null,
                p2Name = null,
                p2Seed = null,
                p2Color = null,
                score1 = null,
                score2 = null,
                penalties1 = null,
                penalties2 = null,
                winnerId = null,
                winnerName = null,
                isCompleted = false
            )
        }
        matchDao.updateMatch(updated)

        if (updated.nextMatchId != null) {
            clearDownstreamMatchSlot(updated.nextMatchId, updated.nextMatchSlot)
        } else if (updated.roundIndex == updated.totalRounds - 1) {
            val t = tournamentDao.getTournamentByIdOnce(match.tournamentId)
            if (t != null) {
                tournamentDao.updateTournament(
                    t.copy(
                        status = TournamentStatus.IN_PROGRESS.name,
                        championId = null,
                        championName = null
                    )
                )
            }
        }
    }

    private fun calculateNextPowerOfTwo(n: Int): Int {
        var p = 2
        while (p < n) {
            p *= 2
        }
        return p
    }

    private fun getRoundName(roundIndex: Int, totalRounds: Int): String {
        val distanceToFinal = totalRounds - 1 - roundIndex
        return when (distanceToFinal) {
            0 -> "Gran Final"
            1 -> "Semifinales"
            2 -> "Cuartos de Final"
            3 -> "Octavos de Final"
            4 -> "16avos de Final"
            else -> "Ronda ${roundIndex + 1}"
        }
    }

    private fun generateSeededBracketSlots(
        participants: List<Participant>,
        bracketSize: Int
    ): List<Participant?> {
        val seedOrder = generateSeedPattern(bracketSize)
        val sorted = participants.sortedBy { it.seed }

        return seedOrder.map { seedNumber ->
            sorted.getOrNull(seedNumber - 1)
        }
    }

    private fun generateSequentialSlots(
        participants: List<Participant>,
        bracketSize: Int
    ): List<Participant?> {
        return List(bracketSize) { index ->
            participants.getOrNull(index)
        }
    }

    private fun generateSeedPattern(size: Int): List<Int> {
        var rounds = listOf(1, 2)
        while (rounds.size < size) {
            val nextSize = rounds.size * 2
            val nextRounds = mutableListOf<Int>()
            for (seed in rounds) {
                nextRounds.add(seed)
                nextRounds.add(nextSize + 1 - seed)
            }
            rounds = nextRounds
        }
        return rounds
    }

    // Domain converters
    private fun TournamentEntity.toDomain() = Tournament(
        id = id,
        name = name,
        status = TournamentStatus.valueOf(status),
        drawMode = DrawMode.valueOf(drawMode),
        totalParticipants = totalParticipants,
        createdAt = createdAt,
        champion = championId?.let { Participant(it, championName ?: "", 0) },
        runnerUp = runnerUpId?.let { Participant(it, runnerUpName ?: "", 0) },
        thirdPlace = thirdPlaceId?.let { Participant(it, thirdPlaceName ?: "", 0) }
    )

    private fun RoundEntity.toDomain() = Round(
        id = id,
        tournamentId = tournamentId,
        roundIndex = roundIndex,
        name = name,
        totalMatches = totalMatches,
        isCompleted = isCompleted
    )

    private fun ParticipantEntity.toDomain() = Participant(
        id = id,
        name = name,
        seed = seed,
        colorHex = colorHex
    )

    private fun MatchEntity.toDomain() = TournamentMatch(
        id = id,
        tournamentId = tournamentId,
        roundId = roundId,
        roundIndex = roundIndex,
        matchIndexInRound = matchIndexInRound,
        totalRounds = totalRounds,
        roundName = roundName,
        participant1 = p1Id?.let { Participant(it, p1Name ?: "", p1Seed ?: 0, p1Color ?: "#6366F1") },
        participant2 = p2Id?.let { Participant(it, p2Name ?: "", p2Seed ?: 0, p2Color ?: "#6366F1") },
        score1 = score1,
        score2 = score2,
        penalties1 = penalties1,
        penalties2 = penalties2,
        winner = winnerId?.let { Participant(it, winnerName ?: "", 0) },
        isBye = isBye,
        isCompleted = isCompleted,
        nextMatchId = nextMatchId,
        nextMatchSlot = nextMatchSlot
    )
}
