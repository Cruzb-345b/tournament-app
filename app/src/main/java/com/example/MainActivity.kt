package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.TournamentViewModel
import com.example.ui.screens.BracketScreen
import com.example.ui.screens.CreateTournamentScreen
import com.example.ui.screens.SorteoDrawScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.TournamentListScreen
import com.example.ui.screens.TournamentManagerScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TournamentApp()
                }
            }
        }
    }
}

@Composable
fun TournamentApp(viewModel: TournamentViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val tournament by viewModel.currentTournament.collectAsState()

    // Handle back button according to design guidelines
    BackHandler(enabled = currentScreen != AppScreen.TOURNAMENT_LIST) {
        when (currentScreen) {
            AppScreen.STATS_VIEW -> viewModel.navigateTo(AppScreen.BRACKET_VIEW)
            AppScreen.BRACKET_VIEW -> viewModel.navigateTo(AppScreen.TOURNAMENT_LIST)
            AppScreen.SORTEO_ANIMATED -> viewModel.navigateTo(AppScreen.TOURNAMENT_LIST)
            AppScreen.CREATE_TOURNAMENT -> viewModel.navigateTo(AppScreen.TOURNAMENT_LIST)
            AppScreen.TOURNAMENT_MANAGER -> viewModel.navigateTo(AppScreen.TOURNAMENT_LIST)
            AppScreen.TOURNAMENT_LIST -> {}
        }
    }

    when (currentScreen) {
        AppScreen.TOURNAMENT_LIST -> {
            TournamentListScreen(
                viewModel = viewModel,
                onCreateNew = { viewModel.navigateTo(AppScreen.CREATE_TOURNAMENT) },
                onSelectTournament = { tId -> viewModel.selectTournament(tId) },
                onOpenManager = { viewModel.navigateTo(AppScreen.TOURNAMENT_MANAGER) }
            )
        }

        AppScreen.CREATE_TOURNAMENT -> {
            CreateTournamentScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateTo(AppScreen.TOURNAMENT_LIST) }
            )
        }

        AppScreen.TOURNAMENT_MANAGER -> {
            TournamentManagerScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateTo(AppScreen.TOURNAMENT_LIST) }
            )
        }

        AppScreen.SORTEO_ANIMATED -> {
            if (tournament != null) {
                SorteoDrawScreen(
                    viewModel = viewModel,
                    tournament = tournament!!,
                    onBack = { viewModel.navigateTo(AppScreen.TOURNAMENT_LIST) },
                    onGoToBracket = { viewModel.navigateTo(AppScreen.BRACKET_VIEW) }
                )
            } else {
                viewModel.navigateTo(AppScreen.TOURNAMENT_LIST)
            }
        }

        AppScreen.BRACKET_VIEW -> {
            if (tournament != null) {
                BracketScreen(
                    viewModel = viewModel,
                    tournament = tournament!!,
                    onBack = { viewModel.navigateTo(AppScreen.TOURNAMENT_LIST) },
                    onViewStats = { viewModel.navigateTo(AppScreen.STATS_VIEW) }
                )
            } else {
                viewModel.navigateTo(AppScreen.TOURNAMENT_LIST)
            }
        }

        AppScreen.STATS_VIEW -> {
            if (tournament != null) {
                StatsScreen(
                    viewModel = viewModel,
                    tournament = tournament!!,
                    onBack = { viewModel.navigateTo(AppScreen.BRACKET_VIEW) }
                )
            } else {
                viewModel.navigateTo(AppScreen.TOURNAMENT_LIST)
            }
        }
    }
}
