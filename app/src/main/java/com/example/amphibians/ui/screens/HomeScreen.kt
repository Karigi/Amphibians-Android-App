package com.example.amphibians.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    // TODO: Home Screen
    // BELOW IS JUST A PLACEHOLDER
    Text(
        text = "Home Screen",
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
            .padding(start = 16.dp, end = 16.dp)
    )
}