package com.example.amphibians.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.amphibians.R
import com.example.amphibians.model.amphibians.Amphibian
import com.example.amphibians.ui.common.AmphibiansTopAppBar
import com.example.amphibians.ui.common.ErrorScreen
import com.example.amphibians.ui.common.LoadingScreen
import com.example.amphibians.ui.screens.amphibians.AmphibianDetails
import com.example.amphibians.ui.screens.amphibians.AmphibiansDataState
import com.example.amphibians.ui.screens.amphibians.AmphibiansViewModel
import com.example.amphibians.utils.AmphibiansAppContentType



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    contentType: AmphibiansAppContentType,
    onCardClick: (Amphibian) -> Unit,
    amphibiansViewModel: AmphibiansViewModel,
    isShowingHomeScreen: Boolean,
    /** ── NEW: snackbarHostState passed in from NavGraph ────────────────
    // We do NOT create a new SnackbarHostState() here with remember {}.
    // We receive the SAME instance that NavGraph created.
    //
    // WHY?
    //   The SharedFlow collector lives in NavGraph. It calls
    //   snackbarHostState.showSnackbar(). For the snackbar to APPEAR,
    //   a SnackbarHost(snackbarHostState) must be rendered somewhere
    //   in the active screen's Scaffold.
    //
    //   If HomeScreen created its OWN SnackbarHostState, the collector
    //   in NavGraph would be calling showSnackbar() on a DIFFERENT object
    //   than what the Scaffold is rendering — the snackbar would never appear.
    //
    //   By sharing the same instance, the NavGraph collector and the
    //   Scaffold's SnackbarHost are connected to the same state.
    */
    snackbarHostState: SnackbarHostState
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()


    /**
     * // ── collectAsStateWithLifecycle() ─────────────────────────────────
     *     // This is how you collect StateFlow in Compose.
     *     //
     *     // What it does:
     *     //   1. Subscribes to the StateFlow as long as the lifecycle is at
     *     //      least in the STARTED state (screen is visible).
     *     //   2. Returns a Compose State<AmphibiansUiState> object.
     *     //   3. Every time the StateFlow emits a new value, this State updates,
     *     //      causing Compose to recompose any composable that reads it.
     *     //   4. When the lifecycle drops below STARTED (app goes to background),
     *     //      collection is paused — no wasted work while not visible.
     *     //   5. When the lifecycle returns to STARTED, collection resumes and
     *     //      the latest value is immediately available (because StateFlow
     *     //      always holds its last value — replay=1).
     *     //
     *     // The `by` delegate:
     *     //   Without `by`: val uiState: State<AmphibiansUiState> = ...
     *     //                 → you'd have to write uiState.value.dataState
     *     //   With    `by`: val uiState: AmphibiansUiState = ...
     *     //                 → you write uiState.dataState directly
     *     //   Both compile to the same bytecode. `by` is just syntactic sugar.
     * */
    val amphibiansUiState by amphibiansViewModel.amphibiansUiState.collectAsStateWithLifecycle()

    /**
     * // rememberSaveable with LazyListState.Saver:
     *     //   - rememberSaveable survives both recomposition AND config changes
     *     //     (rotation, dark mode switch).
     *     //   - LazyListState.Saver knows how to save/restore scroll position.
     *     //   - Without this, rotating the phone would scroll the list back to top.
     *     //   - IMPORTANT: this state is tied to this HomeScreen destination's
     *     //     saveable-state scope. If navigation creates a BRAND-NEW HomeScreen
     *     //     back-stack entry, Compose gives that new entry a new LazyListState,
     *     //     so the list starts from the top again.
     *     //   - That is why NavGraph must reveal the EXISTING HomeScreen with
     *     //     popBackStack() instead of navigating to a new HomeScreen when
     *     //     switching from details route into split mode.
     * */
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() } // For list pane

    /**
     * // ── Long-press callback for snackbar demo ─────────────────────────────
     * // The ViewModel already knows how to emit the one-shot snackbar event:
     * //     onAmphibianLongPressed(amphibian)
     * //
     * // What was missing before this change was the UI gesture itself.
     * // In other words:
     * //   - the ViewModel had the "what should happen" logic
     * //   - but the card had no "listen for long press" logic
     * //
     * // This lambda bridges those two layers:
     * //   Card long-press → call ViewModel → emit SharedFlow event
     * //   → NavGraph collects event → SnackbarHost shows snackbar.
     */
    val onCardLongPress: (Amphibian) -> Unit = { amphibian ->
        amphibiansViewModel.onAmphibianLongPressed(amphibian)
    } // ==> This function can be taken to the NavGraph but for learning purposes, I will use it here
            // to show an example of how lambda functions are used. CHECK DETAILS SCREEN TO SEE ITS IMPLEMENTATION IN THE NAVGRAPH

    val selectedAmphibian = amphibiansViewModel.getSelectedAmphibianFromCache()

    // Reset details scroll state when user picks a new amphibian --> This is for details pane
    val selectedId = amphibiansUiState.selectedAmphibianId
    val scrollState = rememberScrollState()
    // LaunchedEffect(selectedId) restarts its coroutine each time selectedId changes.
    LaunchedEffect(selectedId) {
        scrollState.animateScrollTo(0)
    }




    Scaffold(
        modifier = modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            AmphibiansTopAppBar(
                scrollBehavior = scrollBehavior,
                isShowingHomeScreen = isShowingHomeScreen,
                title = { isDetailPane ->
                    if (isDetailPane) {
                        "Details"
                    } else {
                        "Amphibians"
                    }
                },
                onBackClick = {},
                contentType = contentType
            )
        },
        /**
         * // ── SnackbarHost ───────────────────────────────────────────────
         *         // This is what RENDERS the snackbar visually.
         *         //
         *         // Scaffold reserves a slot at the bottom of the screen for snackbars.
         *         // SnackbarHost(snackbarHostState) tells the Scaffold:
         *         //   "Use this state object to know when to show a snackbar and what
         *         //    message to display."
         *         //
         *         // When NavGraph's collector calls snackbarHostState.showSnackbar("..."),
         *         // this SnackbarHost sees the state change and renders the bar.
         *         //
         *         // The Scaffold also handles padding so the snackbar doesn't overlap
         *         // your content or the navigation bar.
         * */
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->

        when(val dataState = amphibiansUiState.dataState){
            is AmphibiansDataState.Loading -> {
                LoadingScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            is AmphibiansDataState.Success -> {

                when(contentType){
                    AmphibiansAppContentType.LIST_ONLY -> {
                        AmphibiansList(
                            amphibians = dataState.amphibians,
                            modifier = Modifier
                                .fillMaxSize(),
                            contentPadding = paddingValues,
                            onCardClick = onCardClick,
                            onCardLongPress = onCardLongPress,
                            listState = listState,
                            amphibiansViewModel = amphibiansViewModel,
                            isLoadingMore = amphibiansUiState.isLoadingMore,
                            endReached = amphibiansUiState.endReached,
                            pagingErrorMessage = amphibiansUiState.pagingErrorMessage
                        )
                    }
                    AmphibiansAppContentType.LIST_AND_DETAIL -> {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .background(MaterialTheme.colorScheme.background),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            // split layouts should align to top, not center (vertically)
                            verticalAlignment = Alignment.Top,
                        ){
                            // LIST PANE
                            AmphibiansList(
                                amphibians = dataState.amphibians,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onCardClick = onCardClick,
                                onCardLongPress = onCardLongPress,
                                listState = listState,
                                contentPadding = PaddingValues(0.dp),
                                amphibiansViewModel = amphibiansViewModel,
                                isLoadingMore = amphibiansUiState.isLoadingMore,
                                endReached = amphibiansUiState.endReached,
                                pagingErrorMessage = amphibiansUiState.pagingErrorMessage
                            )

                            // RIGHT PANE (DETAILS CONTENT ONLY)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                top = 8.dp,
                                                start = 8.dp,
                                                end = 8.dp
                                            ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    ) {
                                        Text(
                                            text = "Details...",
                                            style = MaterialTheme.typography.titleLarge,
                                            modifier = Modifier
                                                .padding(
                                                    start = 8.dp,
                                                    end = 8.dp
                                                )
                                        )
                                    }

                                    if (selectedAmphibian != null) {
                                        AmphibianDetails(
                                            modifier = Modifier
                                                .fillMaxSize(),
                                            amphibian = selectedAmphibian,
                                            scrollState = scrollState,
                                            // we are still on the home route in split mode:
                                            //  so we want the name to appear in the pane content
                                            isShowingHomeScreen = true,
                                            // Reuse the same long-press callback as the cards so
                                            // the split detail pane and the dedicated details route
                                            // behave consistently.
                                            onLongPress = onCardLongPress,
                                        )
                                    } else {
                                        Text(
                                            text = "No amphibian selected",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .padding(
                                                    start = 8.dp,
                                                    end = 8.dp
                                                )
                                                .fillMaxSize(),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                            }

                        }
                    }
                }
            }

            is AmphibiansDataState.Error -> {
                ErrorScreen(
                    message = dataState.message,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    retryAction = {
                        amphibiansViewModel.refresh()
                    }
                )
            }
        }
    }

}

@Composable
fun AmphibianCard(
    modifier: Modifier = Modifier,
    amphibian: Amphibian,
    onCardClick: (Amphibian) -> Unit,
    onCardLongPress: (Amphibian) -> Unit
){
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            /**
             * combinedClickable lets ONE composable react to multiple gestures.
             *
             * Why not just use Card(onClick = ...)?
             * - Card(onClick = ...) handles normal taps.
             * - But for this feature we also need a long-press gesture.
             * - combinedClickable gives us both in one place:
             *      onClick     -> open/select the amphibian
             *      onLongClick -> fire the snackbar event
             *
             * Resulting flow:
             * 1. User presses and holds the card.
             * 2. onLongClick runs.
             * 3. We call onCardLongPress(amphibian).
             * 4. HomeScreen forwards that to the ViewModel.
             * 5. ViewModel emits AmphibiansUiEvent.ShowSnackbar.
             * 6. NavGraph collects the event and calls showSnackbar(...).
             * 7. Scaffold's SnackbarHost renders the snackbar on screen.
             */
            .combinedClickable(
                onClick = { onCardClick(amphibian) },
                onLongClick = { onCardLongPress(amphibian) }
            )
    ){
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${amphibian.name?: "Unknown"} (${amphibian.type?: "Unknown"})",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            AsyncImage(
                model = ImageRequest.Builder(context = LocalContext.current)
                    .data(amphibian.imgSrc ?: R.drawable.ic_broken_image)
                    .crossfade(true)
                    .build(),
                contentDescription = "${amphibian.name?: "Unknown"} image",
                placeholder = painterResource(R.drawable.loading_img),
                error = painterResource(R.drawable.ic_broken_image),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun AmphibiansList(
    modifier: Modifier = Modifier,
    amphibians: List<Amphibian>,
    onCardClick: (Amphibian) -> Unit,
    onCardLongPress: (Amphibian) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues,
    amphibiansViewModel: AmphibiansViewModel,
    isLoadingMore: Boolean,
    endReached: Boolean,
    pagingErrorMessage: String?
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        itemsIndexed(
            items = amphibians,
            key = { _, amphibian -> amphibian.id } // stable keys
        ) { _, amphibian ->
            AmphibianCard(
                amphibian = amphibian,
                onCardClick = onCardClick,
                onCardLongPress = onCardLongPress,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Footer item (only one)
        item {
            when {
                endReached -> {
                    Text(
                        text = "No more results",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                isLoadingMore -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // Paging error: show message + Retry
                pagingErrorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = pagingErrorMessage,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { amphibiansViewModel.loadNextPage() }) {
                            Text("Retry")
                        }
                    }
                }

                // Normal: Load more
                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(onClick = { amphibiansViewModel.loadNextPage() }) {
                            Text("Load more")
                        }
                    }
                }
            }
        }
    }
}

