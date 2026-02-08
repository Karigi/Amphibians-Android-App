package com.example.amphibians.ui.navigation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.amphibians.ui.screens.HomeScreen
import com.example.amphibians.ui.screens.amphibians.AmphibiansViewModel
import com.example.amphibians.ui.screens.amphibians.DetailsScreen
import com.example.amphibians.utils.AmphibiansAppContentType


private const val TAG = "NavGraph"

enum class AmphibiansNavigationScreen(val route: String){
    HOME_SCREEN("home_screen"),
    DETAILS_SCREEN("details_screen")
}

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    windowSize: WindowWidthSizeClass
) {
    val navController = rememberNavController()

    val amphibiansViewModel: AmphibiansViewModel = hiltViewModel()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val isShowingHomeScreen = currentRoute == AmphibiansNavigationScreen.HOME_SCREEN.route


    val contentType = when(windowSize){
        WindowWidthSizeClass.Compact -> {
            AmphibiansAppContentType.LIST_ONLY
        }
        WindowWidthSizeClass.Medium -> {
            AmphibiansAppContentType.LIST_AND_DETAIL
        }
        WindowWidthSizeClass.Expanded -> {
            AmphibiansAppContentType.LIST_AND_DETAIL
        }
        else -> {
            AmphibiansAppContentType.LIST_ONLY
        }
    }


    NavHost(
        navController = navController,
        startDestination = AmphibiansNavigationScreen.HOME_SCREEN.route, // Change route after testing
        modifier = modifier
            .fillMaxSize()
    ){
        composable(
            route = AmphibiansNavigationScreen.HOME_SCREEN.route
        ){
            HomeScreen(
                modifier = Modifier
                    .fillMaxSize(),
                contentType = contentType,
                isShowingHomeScreen = isShowingHomeScreen,
                amphibiansViewModel = amphibiansViewModel,
                onCardClick = { amphibian ->

                    amphibiansViewModel.selectedAmphibian(amphibian)

                    if (contentType == AmphibiansAppContentType.LIST_ONLY){
                        navController.navigate(
                            AmphibiansNavigationScreen.DETAILS_SCREEN.route
                        )
                    }

                    Log.d(TAG, "onCardClick: $amphibian")

                }
            )
        }

        composable(
            route = AmphibiansNavigationScreen.DETAILS_SCREEN.route
        ){

            // If user rotates/ resizes into split mode while on details route,
            //  immediately navigate back to home screen and show split panes
            if(contentType == AmphibiansAppContentType.LIST_AND_DETAIL){
                LaunchedEffect(Unit) {
                    navController.navigate(AmphibiansNavigationScreen.HOME_SCREEN.route) {
                        popUpTo(AmphibiansNavigationScreen.DETAILS_SCREEN.route) {
                            inclusive = true
                        }
                    }
                }
                return@composable
            }

            // LIST_ONLY: Details route is allowed
            DetailsScreen(
                modifier = Modifier
                    .fillMaxSize(),
                contentType = contentType,
                amphibiansViewModel = amphibiansViewModel,
                isShowingHomeScreen = isShowingHomeScreen,
                onBackClick = {
                    amphibiansViewModel.onBackFromSelectedAmphibian()
                    navController.navigateUp()
                }
            )

            Log.d(TAG, "$contentType")
        }
    }

}