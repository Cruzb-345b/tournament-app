package com.example.util

object SamplePresets {

    data class Preset(
        val title: String,
        val defaultName: String,
        val iconName: String,
        val participants: List<String>
    )

    val presets = listOf(
        Preset(
            title = "Copa de Campeones (8 Equipos)",
            defaultName = "Copa de Campeones Europa",
            iconName = "trophy",
            participants = listOf(
                "Real Madrid", "Manchester City", "Bayern Múnich", "Paris Saint-Germain",
                "Liverpool FC", "Inter de Milán", "FC Barcelona", "Arsenal FC"
            )
        ),
        Preset(
            title = "Torneo E-Sports Pro (16 Equipos)",
            defaultName = "Valorant Masters Invitational",
            iconName = "gamepad",
            participants = listOf(
                "Sentinels", "Fnatic", "Paper Rex", "LOUD",
                "Team Liquid", "DRX", "NRG", "EDward Gaming",
                "Leviatán", "Team Heretics", "T1", "BBL Esports",
                "Gen.G", "Karmine Corp", "Cloud9", "FUT Esports"
            )
        ),
        Preset(
            title = "Pádel Masters Open (8 Parejas)",
            defaultName = "Circuito Pádel Gold Series",
            iconName = "sports_tennis",
            participants = listOf(
                "Galán / Chingotto", "Coello / Tapia", "Stupaczuk / Yanguas", "Paquito / Lebrón",
                "Garrido / Campagnolo", "Nieto / Sanz", "Tello / Belasteguín", "Momo / Alonso"
            )
        ),
        Preset(
            title = "Smash Brothers Arena (8 Luchadores)",
            defaultName = "Smash Ultimate Championship",
            iconName = "sports_kabaddi",
            participants = listOf(
                "Mario", "Link", "Donkey Kong", "Samus",
                "Fox McCloud", "Pikachu", "Captain Falcon", "Marth"
            )
        )
    )
}
