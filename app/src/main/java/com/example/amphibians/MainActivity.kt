package com.example.amphibians

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.Modifier
import com.example.amphibians.ui.navigation.NavGraph
import com.example.amphibians.ui.theme.AmphibiansTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            // set status bar icons to be dark or light based on the color behind it
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            AmphibiansTheme (
                dynamicColor = false
            ){
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                ) {

                    val windowSize = calculateWindowSizeClass(this)

                    NavGraph(
                        windowSize = windowSize.widthSizeClass,
                        modifier = Modifier
                            .fillMaxSize()
                    )

                }
            }
        }
    }
}

