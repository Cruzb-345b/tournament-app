package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DrawMode
import com.example.data.model.Participant
import com.example.data.model.Round
import com.example.data.model.Tournament
import com.example.data.model.TournamentMatch
import com.example.data.model.TournamentStatus
import com.example.ui.TournamentViewModel
import com.example.ui.components.ChampionCelebrationDialog
import com.example.ui.components.MatchCard
import com.example.ui.components.MatchScoreDialog
import com.example.util.SamplePresets
import kotlin.math.log2

/**
 * Pantalla completa en Jetpack Compose que implementa:
 * 1. Entrada de lista interactiva para agregar y gestionar participantes.
 * 2. Representación visual de las rondas del torneo basadas en el estado del repositorio Room.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TournamentManagerScreen(
    viewModel: TournamentViewModel,
    onBack: () -> Unit
) {
    val tournament by viewModel.currentTournament.collectAsState()
    val rounds by viewModel.currentRounds.collectAsState()
    val matches by viewModel.currentMatches.collectAsState()
    val selectedMatchForEdit by viewModel.selectedMatchForEdit.collectAsState()
    val showCelebration by viewModel.showCelebration.collectAsState()

    var tournamentNameInput by remember { mutableStateOf("Torneo Eliminatorio") }
    var participantInputText by remember { mutableStateOf("") }
    val participantsList = remember {
        mutableStateListOf(
            "Equipo Alpha", "Equipo Beta", "Equipo Gamma", "Equipo Delta"
        )
    }

    val focusManager = LocalFocusManager.current

    fun addParticipant() {
        val trimmed = participantInputText.trim()
        if (trimmed.isNotEmpty()) {
            val names = trimmed.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }
            participantsList.addAll(names)
            participantInputText = ""
            focusManager.clearFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Gestor & Visualizador de Rondas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (tournament != null) tournament!!.name else "Configura tus participantes y genera el cuadro",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("manager_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (tournament != null) {
                        IconButton(
                            onClick = { viewModel.resetEntireTournament() },
                            modifier = Modifier.testTag("manager_reset_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reiniciar Torneo")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content inside a single scrollable or partitioned layout
            val mainScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(mainScrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION 1: LIST INPUT FOR ADDING PARTICIPANTS
                ParticipantInputCard(
                    tournamentName = tournamentNameInput,
                    onTournamentNameChange = { tournamentNameInput = it },
                    inputText = participantInputText,
                    onInputTextChange = { participantInputText = it },
                    participants = participantsList,
                    onAddParticipant = { addParticipant() },
                    onRemoveParticipant = { index -> participantsList.removeAt(index) },
                    onClearAll = { participantsList.clear() },
                    onLoadPreset = { preset ->
                        tournamentNameInput = preset.defaultName
                        participantsList.clear()
                        participantsList.addAll(preset.participants)
                    },
                    onGenerateTournament = {
                        viewModel.createTournament(
                            name = tournamentNameInput.ifBlank { "Torneo Eliminatorio" },
                            participants = participantsList.toList(),
                            drawMode = DrawMode.SEEDED
                        )
                    }
                )

                // SECTION 2: VISUAL REPRESENTATION OF TOURNAMENT ROUNDS
                RoundsVisualizerCard(
                    tournament = tournament,
                    rounds = rounds,
                    matches = matches,
                    onMatchClick = { match -> viewModel.openMatchScoreEditor(match) }
                )
            }
        }
    }

    // Modal para editar puntuaciones en tiempo real
    if (selectedMatchForEdit != null) {
        MatchScoreDialog(
            match = selectedMatchForEdit!!,
            onSaveResult = { matchId, s1, s2, p1, p2, wId ->
                viewModel.updateMatchResult(matchId, s1, s2, p1, p2, wId)
            },
            onResetMatch = { matchId ->
                viewModel.resetMatch(matchId)
            },
            onDismiss = { viewModel.closeMatchScoreEditor() }
        )
    }

    // Celebración cuando concluye la Gran Final
    if (showCelebration && tournament?.champion != null) {
        ChampionCelebrationDialog(
            tournament = tournament!!,
            onDismiss = { viewModel.dismissCelebration() },
            onShare = { viewModel.dismissCelebration() }
        )
    }
}

/**
 * Tarjeta de entrada de lista para añadir y administrar participantes
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParticipantInputCard(
    tournamentName: String,
    onTournamentNameChange: (String) -> Unit,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    participants: List<String>,
    onAddParticipant: () -> Unit,
    onRemoveParticipant: (Int) -> Unit,
    onClearAll: () -> Unit,
    onLoadPreset: (SamplePresets.Preset) -> Unit,
    onGenerateTournament: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("participant_input_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "1. Lista de Participantes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (participants.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearAll,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Limpiar", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Presets chips
            Text(
                text = "Plantillas rápidas:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SamplePresets.presets.take(2).forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable { onLoadPreset(preset) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = preset.title, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tournament Name TextField
            OutlinedTextField(
                value = tournamentName,
                onValueChange = onTournamentNameChange,
                label = { Text("Nombre del Torneo") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manager_tournament_name_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Input TextField with Add button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    placeholder = { Text("Escribe jugador / equipo...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("participant_text_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAddParticipant() })
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAddParticipant,
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("add_participant_submit_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Participants Chips list
            Text(
                text = "Participantes actuales (${participants.size}):",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (participants.isEmpty()) {
                Text(
                    text = "Añade al menos 2 participantes para generar el cuadro eliminatorio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            } else {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manager_participant_chips_row"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    participants.forEachIndexed { index, name ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                .padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
                        ) {
                            Text(
                                text = "#${index + 1} $name",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    .clickable { onRemoveParticipant(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Eliminar",
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action: Generate Tournament Bracket Button
            Button(
                onClick = onGenerateTournament,
                enabled = participants.size >= 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("generate_bracket_action_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Generar Cuadro y Crear Rondas (${participants.size} participantes)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Tarjeta de representación visual de las rondas del torneo basadas en el estado del repositorio
 */
@Composable
private fun RoundsVisualizerCard(
    tournament: Tournament?,
    rounds: List<Round>,
    matches: List<TournamentMatch>,
    onMatchClick: (TournamentMatch) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rounds_visualizer_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "2. Representación Visual de Rondas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (tournament != null) {
                        Text(
                            text = "${tournament.name} • ${rounds.size} Rondas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (tournament != null) {
                    val isDone = tournament.status == TournamentStatus.COMPLETED
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isDone) Color(0xFF10B981).copy(alpha = 0.2f)
                                else Color(0xFF6366F1).copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isDone) "🏆 Finalizado" else "⚡ En Juego",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDone) Color(0xFF10B981) else Color(0xFF818CF8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (tournament == null || rounds.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aún no se ha generado ningún cuadro",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Configura los participantes arriba y presiona 'Generar Cuadro y Crear Rondas'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                // Horizontal scrolling tree across all tournament rounds
                val horizontalScroll = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScroll)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    rounds.forEach { round ->
                        val matchesInThisRound = matches.filter { it.roundIndex == round.roundIndex }

                        Column(
                            modifier = Modifier.width(260.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Round Header Pill
                            RoundHeaderBadge(round = round)

                            Spacer(modifier = Modifier.height(10.dp))

                            // Matches in this Round
                            matchesInThisRound.forEach { match ->
                                MatchCard(
                                    match = match,
                                    onClick = { onMatchClick(match) },
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundHeaderBadge(round: Round) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (round.isCompleted) Color(0xFF10B981).copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                1.dp,
                if (round.isCompleted) Color(0xFF10B981).copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = round.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (round.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
        )

        if (round.isCompleted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completada",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Completada", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
            }
        } else {
            Text(
                text = "${round.totalMatches} partidos",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
