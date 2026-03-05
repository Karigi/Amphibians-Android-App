package com.example.amphibians.ui.screens

import androidx.compose.foundation.background
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
    isShowingHomeScreen: Boolean
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()


    val amphibiansUiState by amphibiansViewModel.amphibiansUiState.collectAsStateWithLifecycle()

    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() } // For list pane

    val selectedAmphibian = amphibiansViewModel.getSelectedAmphibianFromCache()

    // Reset details scroll state when user picks a new amphibian --> This is for details pane
    val selectedId = amphibiansUiState.selectedAmphibianId
    val scrollState = rememberScrollState()
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
        }
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
    onCardClick: (Amphibian) -> Unit
){
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        onClick = { onCardClick(amphibian) },
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
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

