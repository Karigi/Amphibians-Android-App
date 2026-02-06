package com.example.amphibians.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.amphibians.utils.AmphibiansAppContentType


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmphibiansTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    isShowingHomeScreen: Boolean ,
    onBackClick: () -> Unit = {},
    title: (Boolean) -> String = { "Error Fetching Title" },
    contentType: AmphibiansAppContentType,
) {


    // Back button should only appear when:
    // We are NOT in the home route(meaning we are on the details route) AND we are in the LIST_ONLY mode
    val showBackButton =
        (!isShowingHomeScreen) && (contentType == AmphibiansAppContentType.LIST_ONLY)


    // Decide ONE title for ONE app bar:
    // LIST_ONLY:
    //  - home route: show list title eg "Amphibians"
    //  - details route: detail title eg amphibian's name
    // LIST_AND_DETAIL:
    //  - always show list title (details pane shows its own header in content)
    val appBarTitle = when (contentType) {
        AmphibiansAppContentType.LIST_ONLY -> {
            if (isShowingHomeScreen) title(false) else title(true)
        }

        AmphibiansAppContentType.LIST_AND_DETAIL -> {
            title(false)
        }
    }

    // maxLines for details
    val detailsMaxLines: Int =
        if (!isShowingHomeScreen || (contentType == AmphibiansAppContentType.LIST_AND_DETAIL) ){
            3
        } else {
            1
        }

    TopAppBar(
        windowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        title = {
            Text(
                text = appBarTitle,
                style = MaterialTheme.typography.headlineLarge,
                maxLines = detailsMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )

}
