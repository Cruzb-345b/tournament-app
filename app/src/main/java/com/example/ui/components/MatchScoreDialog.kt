package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Participant
import com.example.data.model.TournamentMatch

@Composable
fun MatchScoreDialog(
    match: TournamentMatch,
    onSaveResult: (matchId: String, score1: Int?, score2: Int?, pen1: Int?, pen2: Int?, winnerId: String?) -> Unit,
    onResetMatch: (matchId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var score1 by remember { mutableStateOf(match.score1 ?: 0) }
    var score2 by remember { mutableStateOf(match.score2 ?: 0) }
    var showPenalties by remember { mutableStateOf(match.penalties1 != null || match.penalties2 != null) }
    var pen1 by remember { mutableStateOf(match.penalties1 ?: 0) }
    var pen2 by remember { mutableStateOf(match.penalties2 ?: 0) }
    var forcedWinnerId by remember { mutableStateOf<String?>(null) }

    val p1 = match.participant1
    val p2 = match.participant2

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("match_score_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = match.roundName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Partido #${match.matchIndexInRound + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dialog_close_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scoreboard Area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Team 1 Counter
                    ParticipantCounter(
                        participant = p1,
                        score = score1,
                        onIncrement = { score1++ },
                        onDecrement = { if (score1 > 0) score1-- },
                        isWinner = (score1 > score2 && !showPenalties) || (showPenalties && pen1 > pen2) || forcedWinnerId == p1?.id,
                        modifier = Modifier.weight(1f)
                    )

                    // VS divider
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "VS",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    // Team 2 Counter
                    ParticipantCounter(
                        participant = p2,
                        score = score2,
                        onIncrement = { score2++ },
                        onDecrement = { if (score2 > 0) score2-- },
                        isWinner = (score2 > score1 && !showPenalties) || (showPenalties && pen2 > pen1) || forcedWinnerId == p2?.id,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Tie / Penalties Option
                Spacer(modifier = Modifier.height(12.dp))
                if (score1 == score2 && p1 != null && p2 != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedButton(
                            onClick = { showPenalties = !showPenalties },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (showPenalties) "Ocultar Penaltis" else "Desempate por Penaltis")
                        }
                    }
                }

                // Penalties Sub-Scoreboard
                AnimatedVisibility(visible = showPenalties && score1 == score2) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Tanda de Penaltis",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (pen1 > 0) pen1-- }, modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = Icons.Default.Remove, contentDescription = "-1")
                                }
                                Text(
                                    text = "$pen1",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { pen1++ }, modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "+1")
                                }
                            }
                            Text(text = "PEN", color = Color.Gray, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (pen2 > 0) pen2-- }, modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = Icons.Default.Remove, contentDescription = "-1")
                                }
                                Text(
                                    text = "$pen2",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { pen2++ }, modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "+1")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Winner Selection (Direct Advance)
                if (p1 != null && p2 != null) {
                    Text(
                        text = "O avanzar ganador directamente:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onSaveResult(match.id, score1.coerceAtLeast(1), score2, null, null, p1.id)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_win_p1"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(p1.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                onSaveResult(match.id, score1, score2.coerceAtLeast(1), null, null, p2.id)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_win_p2"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(p2.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions: Save & Reset
                Button(
                    onClick = {
                        val winnerId = when {
                            score1 > score2 -> p1?.id
                            score2 > score1 -> p2?.id
                            showPenalties && pen1 > pen2 -> p1?.id
                            showPenalties && pen2 > pen1 -> p2?.id
                            else -> forcedWinnerId
                        }
                        onSaveResult(
                            match.id,
                            score1,
                            score2,
                            if (showPenalties) pen1 else null,
                            if (showPenalties) pen2 else null,
                            winnerId
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_match_result_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Guardar y Avanzar Ganador",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                if (match.isCompleted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { onResetMatch(match.id) },
                        modifier = Modifier.testTag("reset_match_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Reiniciar este partido", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantCounter(
    participant: Participant?,
    score: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    isWinner: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Name
        Text(
            text = participant?.name ?: "TBD",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
            color = if (isWinner) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Big Score with Stepper Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onDecrement),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "-1",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isWinner) Color(0xFF10B981).copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$score",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isWinner) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onIncrement),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "+1",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
