package com.x0550.rubiccubesolver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.x0550.rubiccubesolver.presentation.ui.CubeMainScreen
import com.x0550.rubiccubesolver.presentation.viewmodel.CubeViewModel
import com.x0550.rubiccubesolver.ui.theme.RubicCubeSolverTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val cubeViewModel: CubeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            RubicCubeSolverTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(1_500.milliseconds)
                    showSplash = false
                }

                if (showSplash) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_splash),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .windowInsetsPadding(
                                    WindowInsets.statusBars
                                        .union(WindowInsets.displayCutout)
                                        .only(WindowInsetsSides.Top)
                                )
                                // tappableElement is zero for gesture navigation and non-zero for 3-button navigation
                                .windowInsetsPadding(
                                    WindowInsets.tappableElement
                                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                                )
                                .padding(bottom = dimensionResource(R.dimen.dimen_2dp)),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            CubeMainScreen(
                                viewModel = cubeViewModel,
                                onCloseApp = { finish() },
                                snackbarHostState = snackbarHostState
                            )
                        }

                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .windowInsetsPadding(
                                    WindowInsets.tappableElement.only(WindowInsetsSides.Bottom)
                                )
                                .padding(dimensionResource(R.dimen.dimen_16dp))
                        ) { snackbarData ->
                            Snackbar(
                                snackbarData = snackbarData,
                                containerColor = colorResource(R.color.splash_background),
                                contentColor = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}