package com.compensatuviaje.tracker.feature.trip

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

// Register all routes for the trip flow.
// Data between sub-screens flows through TripViewModel (shared in the back-stack entry);
// NavBackStackEntry arguments are reserved for any minimal routing data added in future.
fun NavGraphBuilder.tripGraph(navController: NavController) {
    composable(route = "trip") {
        TripScreen(navController = navController)
    }
}
