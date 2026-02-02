package com.example.amphibians.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.amphibians.ui.screens.HomeScreen
import com.example.amphibians.utils.AmphibiansAppContentType


enum class AmphibiansNavigationScreen(val route: String){
    HOME_SCREEN("home_screen")
}

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    windowSize: WindowWidthSizeClass
) {
    val navController = rememberNavController()

    val contentType: AmphibiansAppContentType

    when(windowSize){
        WindowWidthSizeClass.Compact -> {
            contentType = AmphibiansAppContentType.LIST_ONLY
        }
        WindowWidthSizeClass.Medium -> {
            contentType = AmphibiansAppContentType.LIST_ONLY
        }
        WindowWidthSizeClass.Expanded -> {
            contentType = AmphibiansAppContentType.LIST_AND_DETAIL
        }
        else -> {
            contentType = AmphibiansAppContentType.LIST_ONLY
        }
    }


    NavHost(
        navController = navController,
        startDestination = AmphibiansNavigationScreen.HOME_SCREEN.route,
        modifier = modifier
            .fillMaxSize()
    ){
        composable(
            route = AmphibiansNavigationScreen.HOME_SCREEN.route
        ){
            HomeScreen(
                modifier = Modifier
                    .fillMaxSize()
                // TODO
            )
        }
    }

}