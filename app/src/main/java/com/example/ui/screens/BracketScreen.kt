package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Tournament
import com.example.data.model.TournamentMatch
import com.example.data.model.TournamentStatus
import com.example.ui.components.ChampionCelebrationDialog
import com.example.ui.components.MatchCard
import com.example.ui.components.MatchScoreDialog
import com.example.ui.TournamentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BracketScreen(
    viewModel: TournamentViewModel,
    tournament: Tournament,
    onBack: () -> Unit,
    onViewStats: () -> Unit
) {
    val context = LocalContext.current
    val matches by viewModel.currentMatches.collectAsState()
    val stats by viewModel.tournamentStats.collectAsState()
    val selectedMatchForEdit by viewModel.selectedMatchForEdit.collectAsState()
    val showCelebration by viewModel.showCelebration.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var viewModePanoramic by remember { mutableStateOf(true) }
    var selectedRoundIndex by remember { mutableIntStateOf(0) }

    val rounds = matches.groupBy { it.roundIndex }.toSortedMap()
    val roundNames = rounds.map { (_, list) -> list.firstOrNull()?.roundName ?: "Ronda" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = tournament.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val statusText = when (tournament.status) {
                                TournamentStatus.COMPLETED -> "¡Completado!"
                                TournamentStatus.IN_PROGRESS -> "En Juego (${stats.completedMatches}/${stats.totalMatches})"
                                TournamentStatus.DRAFT_DRAW -> "Sorteo pendiente"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (tournament.status == TournamentStatus.COMPLETED) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("bracket_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Toggle Panoramic vs Round Tab View
                    IconButton(
                        onClick = { viewModePanoramic = !viewModePanoramic },
                        modifier = Modifier.testTag("toggle_view_mode_button")
                    ) {
                        Icon(
                            imageVector = if (viewModePanoramic) Icons.Default.ViewCarousel else Icons.Default.ViewWeek,
                            contentDescription = "Cambiar vista"
                        )
                    }

                    // Share Bracket
                    IconButton(
                        onClick = { viewModel.shareTournamentBracket(context) },
                        modifier = Modifier.testTag("share_bracket_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Compartir")
                    }

                    // Stats
                    IconButton(
                        onClick = onViewStats,
                        modifier = Modifier.testTag("bracket_stats_button")
                    ) {
                        Icon(imageVector = Icons.Default.BarChart, contentDescription = "Estadísticas")
                    }

                    // More Menu
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Más opciones")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Reiniciar Torneo") },
                            onClick = {
                                showMenu = false
                                showResetConfirm = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        )
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
            // Live Progress Bar Strip
            LinearProgressIndicator(
                progress = { stats.progressPercentage / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = if (tournament.status == TournamentStatus.COMPLETED) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Champion Banner (if completed)
            if (tournament.champion != null) {
                ChampionBanner(
                    championName = tournament.champion.name,
                    runnerUpName = tournament.runnerUp?.name
                )
            }

            // View Mode selection
            if (viewModePanoramic) {
                // Panoramic Bracket (Horizontal scroll with all rounds side by side)
                PanoramicBracketView(
                    rounds = rounds,
                    onMatchClick = { match -> viewModel.openMatchScoreEditor(match) }
                )
            } else {
                // Round by Round Tabs
                if (roundNames.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedRoundIndex.coerceIn(0, (roundNames.size - 1).coerceAtLeast(0)),
                        containerColor = MaterialTheme.colorScheme.surface,
                        edgePadding = 16.dp
                    ) {
                        roundNames.forEachIndexed { index, name ->
                            Tab(
                                selected = selectedRoundIndex == index,
                                onClick = { selectedRoundIndex = index },
                                text = { Text(name, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    val matchesInRound = rounds[selectedRoundIndex] ?: emptyList()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(matchesInRound) { match ->
                            MatchCard(
                                match = match,
                                onClick = { viewModel.openMatchScoreEditor(match) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    // Match Score Editor Dialog
    if (selectedMatchForEdit != null) {
        MatchScoreDialog(
            match = selectedMatchForEdit!!,
            onSaveResult = { mId, s1, s2, p1, p2, wId ->
                viewModel.updateMatchResult(mId, s1, s2, p1, p2, wId)
            },
            onResetMatch = { mId ->
                viewModel.resetMatch(mId)
            },
            onDismiss = { viewModel.closeMatchScoreEditor() }
        )
    }

    // Champion Celebration Modal
    if (showCelebration && tournament.champion != null) {
        ChampionCelebrationDialog(
            tournament = tournament,
            onDismiss = { viewModel.dismissCelebration() },
            onShare = {
                viewModel.dismissCelebration()
                viewModel.shareTournamentBracket(context)
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("¿Reiniciar el Torneo?") },
            text = { Text("Se borrarán todos los resultados y marcadores registrados. Los emparejamientos iniciales volverán a cero.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        viewModel.resetEntireTournament()
                    }
                ) {
                    Text("Reiniciar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun PanoramicBracketView(
    rounds: Map<Int, List<TournamentMatch>>,
    onMatchClick: (TournamentMatch) -> Unit
) {
    val horizontalScrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScrollState)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        rounds.forEach { (roundIdx, matchesList) ->
            val roundTitle = matchesList.firstOrNull()?.roundName ?: "Ronda ${roundIdx + 1}"

            Column(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Round Header pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = roundTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Vertical column of match cards in this round
                val verticalScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(verticalScroll),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    matchesList.forEach { match ->
                        MatchCard(
                            match = match,
                            onClick = { onMatchClick(match) },
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChampionBanner(championName: String, runnerUpName: String?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1B4B)
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFBBF24).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🏆 ¡CAMPEÓN DEL TORNEO!",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = championName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                if (runnerUpName != null) {
                    Text(
                        text = "Subcampeón: $runnerUpName",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}
