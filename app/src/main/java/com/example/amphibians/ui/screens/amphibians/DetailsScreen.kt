package com.example.amphibians.ui.screens.amphibians

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.amphibians.R
import com.example.amphibians.model.amphibians.Amphibian
import com.example.amphibians.ui.common.AmphibiansTopAppBar
import com.example.amphibians.utils.AmphibiansAppContentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    contentType: AmphibiansAppContentType,
    amphibiansViewModel: AmphibiansViewModel,
    isShowingHomeScreen: Boolean
){
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val amphibian = amphibiansViewModel.getSelectedAmphibianFromCache()

    val scrollState = rememberScrollState()


    Scaffold(
        modifier = modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AmphibiansTopAppBar(
                scrollBehavior = scrollBehavior,
                isShowingHomeScreen = isShowingHomeScreen,
                title = { _->
                    "${amphibian?.name?: "Unknown"} (${amphibian?.type?: "Unknown"})"
                },
                onBackClick = onBackClick,
                contentType = contentType,
            )
        }
    ){ paddingValues ->

        AmphibianDetails(
            modifier = Modifier
                .padding(paddingValues),
            amphibian = amphibian,
            scrollState = scrollState,
            isShowingHomeScreen = isShowingHomeScreen
        )

    }

}

@Composable
fun AmphibianDetails(
    modifier: Modifier = Modifier,
    amphibian: Amphibian?,
    scrollState: ScrollState,
    isShowingHomeScreen: Boolean
){
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        if(isShowingHomeScreen){
            Text(
                text = "${amphibian?.name?: "Unknown"} (${amphibian?.type?: "Unknown"})",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(
                        start = 8.dp,
                        end = 8.dp
                    )
            )
        }

        AsyncImage(
            model = ImageRequest.Builder(context = LocalContext.current)
                .data(amphibian?.imgSrc ?: R.drawable.ic_broken_image)
                .crossfade(true)
                .build(),
            contentDescription = "Amphibian image",
            placeholder = painterResource(R.drawable.loading_img),
            error = painterResource(R.drawable.ic_broken_image),
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        bottomStart = 8.dp,
                        bottomEnd = 8.dp
                    )
                )
                .fillMaxWidth()
                .aspectRatio(1f),
            contentScale = ContentScale.Crop
        )

        Text(
            text = amphibian?.description ?: "Unknown",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(
                    start = 8.dp,
                    end = 8.dp
                )
        )
    }
}