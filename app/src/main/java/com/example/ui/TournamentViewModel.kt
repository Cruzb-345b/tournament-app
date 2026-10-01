package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TournamentDatabase
import com.example.data.TournamentRepository
import com.example.data.model.DrawMode
import com.example.data.model.Participant
import com.example.data.model.Round
import com.example.data.model.Tournament
import com.example.data.model.TournamentMatch
import com.example.data.model.TournamentStats
import com.example.data.model.TournamentStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    TOURNAMENT_LIST,
    CREATE_TOURNAMENT,
    SORTEO_ANIMATED,
    BRACKET_VIEW,
    STATS_VIEW,
    TOURNAMENT_MANAGER
}

data class AnimatedDrawPair(
    val matchNumber: Int,
    val participant1: Participant?,
    val participant2: Participant?
)

data class DrawUiState(
    val isSpinning: Boolean = false,
    val remainingPool: List<Participant> = emptyList(),
    val currentDrawn: Participant? = null,
    val completedPairs: List<AnimatedDrawPair> = emptyList(),
    val isFinished: Boolean = false
)

class TournamentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TournamentRepository

    init {
        val database = TournamentDatabase.getDatabase(application)
        repository = TournamentRepository(database)
    }

    val tournaments: StateFlow<List<Tournament>> = repository.allTournaments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentScreen = MutableStateFlow(AppScreen.TOURNAMENT_LIST)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedTournamentId = MutableStateFlow<String?>(null)
    val selectedTournamentId: StateFlow<String?> = _selectedTournamentId.asStateFlow()

    val currentTournament: StateFlow<Tournament?> = _selectedTournamentId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getTournament(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val currentMatches: StateFlow<List<TournamentMatch>> = _selectedTournamentId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getMatches(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val currentParticipants: StateFlow<List<Participant>> = _selectedTournamentId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getParticipants(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val currentRounds: StateFlow<List<Round>> = _selectedTournamentId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getRounds(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val tournamentStats: StateFlow<TournamentStats> = currentMatches.map { matches ->
        val total = matches.size
        val completed = matches.count { it.isCompleted }
        val totalGoals = matches.sumOf { (it.score1 ?: 0) + (it.score2 ?: 0) }
        val progress = if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 0f

        val topMatch = matches
            .filter { it.score1 != null && it.score2 != null }
            .maxByOrNull { (it.score1 ?: 0) + (it.score2 ?: 0) }
            ?.let { m ->
                "${m.participant1?.name ?: "TBD"} ${m.score1} - ${m.score2} ${m.participant2?.name ?: "TBD"}"
            }

        TournamentStats(
            totalMatches = total,
            completedMatches = completed,
            totalGoalsOrPoints = totalGoals,
            progressPercentage = progress,
            highestScoringMatch = topMatch
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TournamentStats(0, 0, 0, 0f)
    )

    // Match Edit Dialog State
    private val _selectedMatchForEdit = MutableStateFlow<TournamentMatch?>(null)
    val selectedMatchForEdit: StateFlow<TournamentMatch?> = _selectedMatchForEdit.asStateFlow()

    // Champion Celebration Dialog State
    private val _showCelebration = MutableStateFlow(false)
    val showCelebration: StateFlow<Boolean> = _showCelebration.asStateFlow()

    // Sorteo / Animated Draw State
    private val _drawState = MutableStateFlow(DrawUiState())
    val drawState: StateFlow<DrawUiState> = _drawState.asStateFlow()
    private var drawJob: Job? = null

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectTournament(tournamentId: String) {
        _selectedTournamentId.value = tournamentId
        viewModelScope.launch {
            // Check status to route appropriately
            val t = tournaments.value.find { it.id == tournamentId }
            if (t?.status == TournamentStatus.DRAFT_DRAW) {
                prepareAnimatedDraw(tournamentId)
                _currentScreen.value = AppScreen.SORTEO_ANIMATED
            } else {
                _currentScreen.value = AppScreen.BRACKET_VIEW
            }
        }
    }

    fun createTournament(name: String, participants: List<String>, drawMode: DrawMode) {
        viewModelScope.launch {
            val tId = repository.createTournament(name, participants, drawMode)
            _selectedTournamentId.value = tId
            if (drawMode == DrawMode.RANDOM_ANIMATED) {
                prepareAnimatedDraw(tId)
                _currentScreen.value = AppScreen.SORTEO_ANIMATED
            } else {
                _currentScreen.value = AppScreen.BRACKET_VIEW
            }
        }
    }

    private fun prepareAnimatedDraw(tournamentId: String) {
        viewModelScope.launch {
            // Get participants
            delay(100) // slight buffer for db read
            val pList = currentParticipants.value.shuffled()
            _drawState.value = DrawUiState(
                isSpinning = false,
                remainingPool = pList,
                currentDrawn = null,
                completedPairs = emptyList(),
                isFinished = false
            )
        }
    }

    /**
     * Steps the interactive lottery drum (sorteo) by 1 participant or finishes all
     */
    fun drawNextBall() {
        if (_drawState.value.isSpinning || _drawState.value.isFinished) return

        drawJob?.cancel()
        drawJob = viewModelScope.launch {
            val currentPool = _drawState.value.remainingPool.toMutableList()
            if (currentPool.isEmpty()) {
                _drawState.value = _drawState.value.copy(isFinished = true)
                return@launch
            }

            _drawState.value = _drawState.value.copy(isSpinning = true)

            // Simulate roulette/bombo spinning suspense
            for (i in 0 until 5) {
                val tempPick = currentPool.random()
                _drawState.value = _drawState.value.copy(currentDrawn = tempPick)
                delay(120)
            }

            val chosen = currentPool.removeAt(0)
            val currentPairs = _drawState.value.completedPairs.toMutableList()

            // See if last pair has a slot open
            val lastPair = currentPairs.lastOrNull()
            if (lastPair != null && lastPair.participant2 == null) {
                // Fill slot 2
                currentPairs[currentPairs.lastIndex] = lastPair.copy(participant2 = chosen)
            } else {
                // New match pair
                currentPairs.add(
                    AnimatedDrawPair(
                        matchNumber = currentPairs.size + 1,
                        participant1 = chosen,
                        participant2 = null
                    )
                )
            }

            val isDone = currentPool.isEmpty()
            _drawState.value = _drawState.value.copy(
                isSpinning = false,
                remainingPool = currentPool,
                currentDrawn = chosen,
                completedPairs = currentPairs,
                isFinished = isDone
            )

            if (isDone) {
                applyFinalDrawResults(currentPairs)
            }
        }
    }

    fun instantDrawAll() {
        drawJob?.cancel()
        viewModelScope.launch {
            _drawState.value = _drawState.value.copy(isSpinning = true)
            delay(300)

            val pool = currentParticipants.value.shuffled()
            val pairs = mutableListOf<AnimatedDrawPair>()
            var pairNum = 1

            for (i in pool.indices step 2) {
                val p1 = pool.getOrNull(i)
                val p2 = pool.getOrNull(i + 1)
                pairs.add(AnimatedDrawPair(matchNumber = pairNum++, participant1 = p1, participant2 = p2))
            }

            _drawState.value = DrawUiState(
                isSpinning = false,
                remainingPool = emptyList(),
                currentDrawn = pool.lastOrNull(),
                completedPairs = pairs,
                isFinished = true
            )

            applyFinalDrawResults(pairs)
        }
    }

    private suspend fun applyFinalDrawResults(pairs: List<AnimatedDrawPair>) {
        val tId = _selectedTournamentId.value ?: return
        val ordered = mutableListOf<Participant>()
        for (pair in pairs) {
            pair.participant1?.let { ordered.add(it) }
            pair.participant2?.let { ordered.add(it) }
        }
        repository.applyDrawOrder(tId, ordered)
    }

    fun openMatchScoreEditor(match: TournamentMatch) {
        _selectedMatchForEdit.value = match
    }

    fun closeMatchScoreEditor() {
        _selectedMatchForEdit.value = null
    }

    fun updateMatchResult(
        matchId: String,
        score1: Int?,
        score2: Int?,
        penalties1: Int? = null,
        penalties2: Int? = null,
        forcedWinnerId: String? = null
    ) {
        viewModelScope.launch {
            repository.updateMatchResult(
                matchId = matchId,
                score1 = score1,
                score2 = score2,
                penalties1 = penalties1,
                penalties2 = penalties2,
                forcedWinnerId = forcedWinnerId
            )
            _selectedMatchForEdit.value = null

            // Check if final was completed to trigger celebration
            delay(150)
            val updated = currentTournament.value
            if (updated?.champion != null && updated.status == TournamentStatus.COMPLETED) {
                _showCelebration.value = true
            }
        }
    }

    fun resetMatch(matchId: String) {
        viewModelScope.launch {
            repository.resetMatchResult(matchId)
            _selectedMatchForEdit.value = null
        }
    }

    fun dismissCelebration() {
        _showCelebration.value = false
    }

    fun resetEntireTournament() {
        val tId = _selectedTournamentId.value ?: return
        viewModelScope.launch {
            repository.resetTournament(tId)
            _showCelebration.value = false
        }
    }

    fun deleteTournament(tournamentId: String) {
        viewModelScope.launch {
            repository.deleteTournament(tournamentId)
            if (_selectedTournamentId.value == tournamentId) {
                _selectedTournamentId.value = null
                _currentScreen.value = AppScreen.TOURNAMENT_LIST
            }
        }
    }

    fun shareTournamentBracket(context: Context) {
        val tournament = currentTournament.value ?: return
        val matches = currentMatches.value
        val stats = tournamentStats.value

        val rounds = matches.groupBy { it.roundName }
        val sb = StringBuilder()
        sb.append("🏆 *${tournament.name}* 🏆\n")
        sb.append("Estado: ${if (tournament.status == TournamentStatus.COMPLETED) "¡FINALIZADO!" else "EN CURSO"}\n")
        if (tournament.champion != null) {
            sb.append("🥇 Campeón: *${tournament.champion.name}*\n")
            tournament.runnerUp?.let { sb.append("🥈 Subcampeón: ${it.name}\n") }
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━\n\n")

        rounds.forEach { (roundName, roundMatches) ->
            sb.append("📍 *$roundName*\n")
            roundMatches.forEach { m ->
                val p1 = m.participant1?.name ?: "TBD"
                val p2 = m.participant2?.name ?: (if (m.isBye) "PASE DIRECTO" else "TBD")
                val score = if (m.score1 != null && m.score2 != null) {
                    val pen = if (m.penalties1 != null && m.penalties2 != null) " (pen. ${m.penalties1}-${m.penalties2})" else ""
                    " [${m.score1} - ${m.score2}$pen]"
                } else ""
                val winnerStar = if (m.winner != null) " ⭐ Ganador: ${m.winner.name}" else ""
                sb.append("• $p1 vs $p2$score$winnerStar\n")
            }
            sb.append("\n")
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("📊 Partidos Jugados: ${stats.completedMatches}/${stats.totalMatches} (${stats.progressPercentage.toInt()}%)\n")
        sb.append("⚡ Goles/Puntos Totales: ${stats.totalGoalsOrPoints}\n")
        sb.append("Generado con TorneoPro Android")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Compartir Cuadro del Torneo")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
