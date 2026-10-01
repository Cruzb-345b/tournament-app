package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Participant
import com.example.data.model.TournamentMatch

@Composable
fun MatchCard(
    match: TournamentMatch,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReadyToPlay = match.participant1 != null && match.participant2 != null && !match.isCompleted && !match.isBye
    val isWaiting = (match.participant1 == null || match.participant2 == null) && !match.isBye
    val isFinal = match.roundIndex == match.totalRounds - 1

    val borderColor by animateColorAsState(
        targetValue = when {
            match.isCompleted -> Color(0xFF10B981) // Emerald
            isReadyToPlay -> MaterialTheme.colorScheme.primary // Electric indigo
            isFinal -> Color(0xFFF59E0B) // Gold
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        },
        label = "matchBorderColor"
    )

    Card(
        modifier = modifier
            .width(260.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (match.isCompleted || isReadyToPlay) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = match.participant1 != null || match.participant2 != null, onClick = onClick)
            .testTag("match_card_${match.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isReadyToPlay) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row: Match status & Round info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "M${match.matchIndexInRound + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )

                MatchStatusBadge(
                    isCompleted = match.isCompleted,
                    isReady = isReadyToPlay,
                    isWaiting = isWaiting,
                    isBye = match.isBye,
                    isFinal = isFinal
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Participant 1
            ParticipantRow(
                participant = match.participant1,
                score = match.score1,
                penalties = match.penalties1,
                isWinner = match.winner != null && match.winner.id == match.participant1?.id,
                isLoser = match.isCompleted && match.winner != null && match.winner.id != match.participant1?.id,
                isByeSlot = false
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Divider line with subtle VS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Participant 2
            ParticipantRow(
                participant = match.participant2,
                score = match.score2,
                penalties = match.penalties2,
                isWinner = match.winner != null && match.winner.id == match.participant2?.id,
                isLoser = match.isCompleted && match.winner != null && match.winner.id != match.participant2?.id,
                isByeSlot = match.isBye && match.participant2 == null
            )
        }
    }
}

@Composable
private fun ParticipantRow(
    participant: Participant?,
    score: Int?,
    penalties: Int?,
    isWinner: Boolean,
    isLoser: Boolean,
    isByeSlot: Boolean
) {
    val alpha = if (isLoser) 0.5f else 1.0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isWinner) Color(0xFF10B981).copy(alpha = 0.15f)
                else Color.Transparent
            )
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Avatar / Seed tag
            if (participant != null) {
                val color = runCatching { Color(android.graphics.Color.parseColor(participant.colorHex)) }
                    .getOrDefault(MaterialTheme.colorScheme.primary)

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = alpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (participant.seed > 0) "#${participant.seed}" else participant.name.take(1),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = participant.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
                        color = if (isWinner) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (isByeSlot) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "—", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PASE DIRECTO (BYE)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "?", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Por definir",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Score display
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isWinner) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Ganador",
                    tint = Color(0xFF10B981),
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 4.dp)
                )
            }

            if (score != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isWinner) Color(0xFF10B981).copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surface
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    val scoreText = if (penalties != null) "$score ($penalties)" else "$score"
                    Text(
                        text = scoreText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isWinner) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isWinner) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchStatusBadge(
    isCompleted: Boolean,
    isReady: Boolean,
    isWaiting: Boolean,
    isBye: Boolean,
    isFinal: Boolean
) {
    val (label, bg, fg) = when {
        isBye -> Triple("BYE", Color(0xFF64748B).copy(alpha = 0.2f), Color(0xFF94A3B8))
        isCompleted -> Triple("Finalizado", Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF10B981))
        isReady && isFinal -> Triple("¡GRAN FINAL!", Color(0xFFF59E0B).copy(alpha = 0.2f), Color(0xFFF59E0B))
        isReady -> Triple("Jugar Ahora", Color(0xFF6366F1).copy(alpha = 0.2f), Color(0xFF818CF8))
        else -> Triple("Esperando", Color(0xFF475569).copy(alpha = 0.2f), Color(0xFF94A3B8))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}
