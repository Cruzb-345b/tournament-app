package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DrawMode
import com.example.ui.TournamentViewModel
import com.example.util.SamplePresets
import kotlin.math.log2
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateTournamentScreen(
    viewModel: TournamentViewModel,
    onBack: () -> Unit
) {
    var tournamentName by remember { mutableStateOf("Copa Relámpago 2026") }
    var participantInput by remember { mutableStateOf("") }
    val participants = remember {
        mutableStateListOf(
            "Real Madrid", "Manchester City", "Bayern Múnich", "Paris Saint-Germain",
            "Liverpool FC", "Inter de Milán", "FC Barcelona", "Arsenal FC"
        )
    }
    var selectedDrawMode by remember { mutableStateOf(DrawMode.RANDOM_ANIMATED) }

    fun addParticipantsFromInput() {
        val raw = participantInput.trim()
        if (raw.isNotEmpty()) {
            val items = raw.split("\n", ",").map { it.trim() }.filter { it.isNotBlank() }
            participants.addAll(items)
            participantInput = ""
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configurar Torneo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("create_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Preset Templates Card
            Text(
                text = "Plantillas Rápidas de Ejemplo",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SamplePresets.presets.forEach { preset ->
                    OutlinedButton(
                        onClick = {
                            tournamentName = preset.defaultName
                            participants.clear()
                            participants.addAll(preset.participants)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("preset_${preset.title.take(10)}")
                    ) {
                        Text(text = preset.title, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Tournament Name Input
            OutlinedTextField(
                value = tournamentName,
                onValueChange = { tournamentName = it },
                label = { Text("Nombre del Torneo") },
                placeholder = { Text("Ej: Champions League, Torneo FIFA...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tournament_name_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Draw Mode Selector
            Text(
                text = "Modalidad del Sorteo (Winner Bracket)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DrawModeCard(
                    title = "Sorteo Tómbola",
                    subtitle = "Bombo animado al azar",
                    icon = Icons.Default.Casino,
                    isSelected = selectedDrawMode == DrawMode.RANDOM_ANIMATED,
                    onClick = { selectedDrawMode = DrawMode.RANDOM_ANIMATED },
                    modifier = Modifier.weight(1f)
                )

                DrawModeCard(
                    title = "Sembrados",
                    subtitle = "Cabezas de serie (1 vs 8)",
                    icon = Icons.Default.FormatListNumbered,
                    isSelected = selectedDrawMode == DrawMode.SEEDED,
                    onClick = { selectedDrawMode = DrawMode.SEEDED },
                    modifier = Modifier.weight(1f)
                )

                DrawModeCard(
                    title = "Manual",
                    subtitle = "Orden de registro",
                    icon = Icons.Default.Shuffle,
                    isSelected = selectedDrawMode == DrawMode.MANUAL,
                    onClick = { selectedDrawMode = DrawMode.MANUAL },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Add Participants Section
            Text(
                text = "Participantes (${participants.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = participantInput,
                    onValueChange = { participantInput = it },
                    placeholder = { Text("Nombre del equipo o jugador...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("participant_input_field"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { addParticipantsFromInput() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_participant_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Añadir")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Participant Chips
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("participants_chips_container"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                participants.forEachIndexed { index, name ->
                    ParticipantChip(
                        name = name,
                        seed = index + 1,
                        onRemove = { participants.removeAt(index) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bracket Structure Helper Info
            val pCount = participants.size
            val nextPower = calculatePowerOfTwo(pCount)
            val byesCount = if (pCount in 2 until nextPower) nextPower - pCount else 0
            val roundsCount = (log2(nextPower.toDouble())).toInt().coerceAtLeast(1)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Estructura del Cuadro",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (pCount < 2) "Agrega al menos 2 participantes para comenzar."
                            else "Cuadro de $nextPower casillas en $roundsCount rondas.${if (byesCount > 0) " ($byesCount clasificarán directo por BYE)" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    if (participants.size >= 2) {
                        viewModel.createTournament(
                            name = tournamentName.ifBlank { "Torneo Pro" },
                            participants = participants.toList(),
                            drawMode = selectedDrawMode
                        )
                    }
                },
                enabled = participants.size >= 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_tournament_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (selectedDrawMode == DrawMode.RANDOM_ANIMATED) "Comenzar Sorteo de Cruces 🎲" else "Generar Cuadro Eliminatorio 🏆",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DrawModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ParticipantChip(
    name: String,
    seed: Int,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#$seed $name",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                .clickable(onClick = onRemove),
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

private fun calculatePowerOfTwo(n: Int): Int {
    if (n <= 2) return 2
    var p = 2
    while (p < n) {
        p *= 2
    }
    return p
}
