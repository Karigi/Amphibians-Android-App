package com.example.amphibians.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.amphibians.ui.screens.HomeScreen
import com.example.amphibians.ui.screens.amphibians.AmphibiansUiEvent
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

    /**
     * // ── Single ViewModel instance shared across all screens ───────────
     *     // hiltViewModel() here (at NavGraph level) means the ViewModel is
     *     // scoped to the NavGraph composable's lifetime — it lives as long as
     *     // the NavGraph is in the composition (i.e., the whole app session).
     *     // Both HomeScreen and DetailsScreen receive the SAME instance.
     * */
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

    //==================PREPARING TO COLLECT THE SHAREDFLOW==================
    /**
     * // ── SnackbarHostState ─────────────────────────────────────────────
     *     // remember() keeps this object alive across recompositions.
     *     // We create it HERE (at NavGraph level) so it can be passed to:
     *     //   1. The SharedFlow collector below (to trigger snackbars from ViewModel events)
     *     //   2. HomeScreen's Scaffold (to actually render the SnackbarHost)
     *     //   3. DetailsScreen's Scaffold (same)
     *     //
     *     // Having ONE SnackbarHostState means only one snackbar shows at a time,
     *     // even if events fire rapidly. showSnackbar() is a suspend function that
     *     // queues them automatically.
     * */
    val snackbarHostState = remember { SnackbarHostState() }

    /**
     * // ── lifecycleOwner ────────────────────────────────────────────────
     *     // We need the LifecycleOwner to use repeatOnLifecycle.
     *     // LocalLifecycleOwner.current gives us the nearest lifecycle owner
     *     // in the composition tree (usually the Activity).
     * */
    val lifecycleOwner = LocalLifecycleOwner.current

    /**
     * // ── Collecting SharedFlow (one-shot UI events) ────────────────────
     *     //
     *     // WHY LaunchedEffect here at NavGraph level (not inside each screen)?
     *     //
     *     //   Because navController lives here. Navigation events from the ViewModel
     *     //   must call navController.navigate(), which is only accessible here.
     *     //   Also, collecting here means the collector is ALWAYS alive as long as
     *     //   the NavGraph exists — we won't miss events just because a specific
     *     //   screen composable hasn't been composed yet.
     *     //
     *     // WHY LaunchedEffect(viewModel, lifecycleOwner) with those two keys?
     *     //
     *     //   LaunchedEffect cancels and restarts its coroutine whenever its
     *     //   key(s) change. We use:
     *     //     - viewModel: if somehow the ViewModel reference changes, restart.
     *     //     - lifecycleOwner: if the lifecycle owner changes, restart.
     *     //   In practice, both are stable for the app's lifetime, so this
     *     //   coroutine starts once and never restarts.
     *     //
     *     //   Using LaunchedEffect(Unit) would also work here since viewModel
     *     //   and lifecycleOwner never change, but being explicit is safer.
     *     //
     *     // WHY repeatOnLifecycle(Lifecycle.State.STARTED)?
     *     //
     *     //   This is the most important part. Here is what it does:
     *     //
     *     //   STARTED means: "collect only when the app is in the foreground
     *     //   and visible to the user."
     *     //
     *     //   Timeline:
     *     //   App comes to foreground → lifecycle reaches STARTED
     *     //     → inner block starts → SharedFlow.collect() begins listening
     *     //   User presses home / another app covers ours → lifecycle drops below STARTED
     *     //     → inner block is CANCELLED → collect() stops
     *     //   User returns to app → lifecycle reaches STARTED again
     *     //     → inner block RESTARTS → collect() begins listening again
     *     //
     *     //   WHY this matters for our events:
     *     //     If a snackbar event fires while the app is in the background,
     *     //     the collector is paused and does NOT process it.
     *     //     When the user returns, a NEW collect() starts — since SharedFlow
     *     //     has replay=0, the missed event is gone. This is INTENTIONAL.
     *     //     You do NOT want a snackbar appearing the moment the user returns
     *     //     from checking another app — that would be confusing.
     *     //
     *     //   For navigation events, this is even more critical:
     *     //     If NavigateToDetails fired while in the background and was processed,
     *     //     the user would return to find themselves already on the details screen.
     *     //     That's terrible UX. repeatOnLifecycle(STARTED) prevents this.
     *     //
     *     // WHY NOT just LaunchedEffect { viewModel.amphibiansUiEvents.collect {} }
     *     //   without repeatOnLifecycle?
     *     //
     *     //   Without repeatOnLifecycle, the collect() keeps running even when
     *     //   the app is in the background. For navigation, this could cause
     *     //   navController.navigate() to be called while no screen is active,
     *     //   which can crash or corrupt the back stack.
     * */
    LaunchedEffect(amphibiansViewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
            // .collect {} suspends here, waiting for the next event.
            // Each time the ViewModel calls _amphibiansUiEvents.emit(...),
            // this block wakes up, handles the event, then suspends again.
            amphibiansViewModel.amphibiansUiEvents.collect { event ->
                Log.d(TAG, "NavGraph UI event: \n $event")
                when(event) {
                    // ── Navigation event ──────────────────────────────
                    // The ViewModel told us to navigate to the detail screen.
                    // We only do this for LIST_ONLY mode — in LIST_AND_DETAIL
                    // (split pane) there is no separate details route; the
                    // detail pane is already shown inline in HomeScreen.
                    is AmphibiansUiEvent.NavigateToDetails -> {
                        if (contentType == AmphibiansAppContentType.LIST_ONLY) {
                            navController.navigate(
                                AmphibiansNavigationScreen.DETAILS_SCREEN.route
                            ) {
                                // launchSingleTop = true: if the details screen
                                // is already on top of the back stack (e.g. user
                                // tapped the same card twice very quickly), do NOT
                                // push a second copy. Just re-use the existing one.
                                launchSingleTop = true
                            }
                        }
                        // In LIST_AND_DETAIL mode: we do nothing here.
                        // The selected amphibian ID was already updated in StateFlow
                        // by selectedAmphibian(), and the split pane in HomeScreen
                        // recomposes automatically to show the new selection.
                    }
                    // ── Navigate back event ───────────────────────────
                    // Currently triggered manually via onBackClick in the UI,
                    // but if the ViewModel ever needs to force a back navigation
                    // (e.g., after a delete), it can emit NavigateBack.
                    is AmphibiansUiEvent.NavigateBack -> {
                        navController.navigateUp()
                    }
                    // ── Snackbar event ────────────────────────────────
                    // showSnackbar() is a suspend function. It suspends until
                    // the snackbar is dismissed (either by the user or timeout).
                    //
                    // This means: if two ShowSnackbar events fire quickly,
                    // the second one waits in the SharedFlow's buffer (capacity=1)
                    // until the first snackbar is dismissed.
                    //
                    // If a third fires before the second is consumed,
                    // DROP_OLDEST kicks in and the second is dropped.
                    // The user sees the first snackbar, then the third.
                    // This is acceptable for "copied to clipboard" style messages.
                    is AmphibiansUiEvent.ShowSnackbar -> {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            // SHORT: auto-dismisses after ~4 seconds
                            // LONG: auto-dismisses after ~10 seconds
                            // INDEFINITE: stays until user dismisses
                            duration = SnackbarDuration.Long,
                            // a boolean to show a dismiss action in the Snackbar.
                            // This is recommended to be set to true for better
                            // accessibility when a Snackbar is set with a
                            // SnackbarDuration.Indefinite
                            withDismissAction = true
                        )
                    }
                }
            }
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
                // ── snackbarHostState passed down ─────────────────────
                // HomeScreen's Scaffold will render the SnackbarHost using
                // this state object. When showSnackbar() is called above,
                // the Scaffold here displays it visually.
                snackbarHostState = snackbarHostState,
                onCardClick = { amphibian ->

                    // selectedAmphibian() does two things:
                    //   1. Updates StateFlow (selectedAmphibianId) — persistent
                    //   2. Emits NavigateToDetails via SharedFlow — one-shot
                    // The SharedFlow collector above handles the actual navigation.
                    amphibiansViewModel.selectedAmphibian(amphibian)

                    // ==>Navigate to details screen already handled in LaunchedEffect
                    //      which collects the SharedFlow


                    Log.d(TAG, "onCardClick: $amphibian")

                }
            )
        }

        composable(
            route = AmphibiansNavigationScreen.DETAILS_SCREEN.route
        ){

            // If the user rotates/resizes into split mode while already sitting on
            // the details route, we should stop showing the separate details screen
            // and go back to the split HomeScreen.
            //
            // BEST FIX:
            //   Prefer popBackStack() when HomeScreen is directly underneath.
            //
            // WHY this preserves the list scroll:
            //   HomeScreen already exists in the back stack and already owns the
            //   rememberSaveable(LazyListState.Saver) entry for the list.
            //   Popping DetailsScreen simply reveals that ORIGINAL HomeScreen.
            //   No new destination is created, so the list keeps its old position.
            //
            // WHY navigate(home) was the problem before:
            //   navigate(home) can create a NEW HomeScreen destination.
            //   A new destination means a new saveable-state scope.
            //   A new saveable-state scope means a fresh LazyListState.
            //   A fresh LazyListState starts at the top of the list.
            //
            // FALLBACK:
            //   If there is no existing HomeScreen under DetailsScreen
            //   (for example, after an unusual deep link / restored stack),
            //   then we navigate to HomeScreen as a safe fallback.
            if(contentType == AmphibiansAppContentType.LIST_AND_DETAIL){
                /**
                 * If we used LaunchedEffect(contentType), it would fire every time
                 * //   contentType changes. But we're already inside
                 * //   if (contentType == LIST_AND_DETAIL), so it would only be composed
                 * //   when contentType IS LIST_AND_DETAIL. By the time it's composed,
                 * //   contentType is already LIST_AND_DETAIL.
                 * //   Any further contentType changes while we're still on DETAILS_SCREEN
                 * //   and in LIST_AND_DETAIL mode would retrigger navigation. Since we're
                 * //   navigating away immediately, this composable leaves composition and
                 * //   the LaunchedEffect is canceled anyway. So Unit is the right key.
                 * */

                //====NEVER CALL SIDE EFFECTS DIRECTLY IN A COMPOSE BODY(NAVIGATION, LOGGING, NETWORK CALLS)
                LaunchedEffect(Unit) {
                    val canRevealExistingHome =
                        navController.previousBackStackEntry?.destination?.route ==
                            AmphibiansNavigationScreen.HOME_SCREEN.route

                    if (canRevealExistingHome) {
                        // Remove DetailsScreen and expose the ORIGINAL HomeScreen.
                        // This is the path that keeps the list scroll intact.
                        navController.popBackStack()
                    } else {
                        // Defensive fallback only.
                        // We are NOT depending on this branch to preserve the list
                        // scroll bug described above; the real fix is the popBackStack()
                        // branch. This branch just guarantees the user still lands on
                        // HomeScreen even if the stack shape is unexpected.
                        navController.navigate(AmphibiansNavigationScreen.HOME_SCREEN.route) {
                            launchSingleTop = true
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
                // ── snackbarHostState passed down ─────────────────────
                // DetailsScreen's Scaffold will render the SnackbarHost.
                // The SAME state object as HomeScreen — one queue, one
                // snackbar at a time, regardless of which screen is active.
                snackbarHostState = snackbarHostState,
                onBackClick = {
                    amphibiansViewModel.onBackFromSelectedAmphibian()
                    navController.navigateUp()
                },
                onPageLongPress = { amphibian ->
                    amphibiansViewModel.onAmphibianLongPressed(amphibian)
                }
            )

            Log.d(TAG, "$contentType")
        }
    }

}