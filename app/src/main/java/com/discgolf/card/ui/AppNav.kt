package com.discgolf.card.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.discgolf.card.ui.history.HistoryScreen
import com.discgolf.card.ui.result.RoundDetailScreen
import com.discgolf.card.ui.result.SummaryScreen
import com.discgolf.card.ui.scorecard.ScorecardScreen
import com.discgolf.card.ui.setup.SetupScreen
import kotlinx.serialization.Serializable

@Serializable
data object Setup

@Serializable
data class Scorecard(val roundId: String)

@Serializable
data class Summary(val roundId: String)

@Serializable
data object History

@Serializable
data class RoundDetail(val roundId: String)

@Composable
fun AppNav() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Setup,
        enterTransition = {
            fadeIn(tween(200)) + slideInHorizontally(tween(200)) { it / 20 }
        },
        exitTransition = { fadeOut(tween(120)) },
        popEnterTransition = {
            fadeIn(tween(200)) + slideInHorizontally(tween(200)) { -it / 20 }
        },
        popExitTransition = {
            fadeOut(tween(120)) + slideOutHorizontally(tween(200)) { it / 20 }
        }
    ) {
        composable<Setup> {
            SetupScreen(
                onOpenScorecard = { navController.navigate(Scorecard(it)) },
                onOpenDraft = { navController.navigate(Scorecard(it)) },
                onOpenHistory = { navController.navigate(History) }
            )
        }

        composable<Scorecard> { entry ->
            val route = entry.toRoute<Scorecard>()
            ScorecardScreen(
                roundId = route.roundId,
                onBack = { navController.popBackStack() },
                onFinished = {
                    navController.navigate(Summary(route.roundId)) {
                        popUpTo<Scorecard> { inclusive = true }
                    }
                }
            )
        }

        composable<Summary> { entry ->
            val route = entry.toRoute<Summary>()
            SummaryScreen(
                roundId = route.roundId,
                onDone = {
                    navController.navigate(Setup) {
                        popUpTo<Setup> { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onViewHistory = { navController.navigate(History) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<History> {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onRoundClick = { navController.navigate(RoundDetail(it)) },
                onStartRound = {
                    navController.navigate(Setup) {
                        popUpTo<Setup> { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<RoundDetail> { entry ->
            val route = entry.toRoute<RoundDetail>()
            RoundDetailScreen(
                roundId = route.roundId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
