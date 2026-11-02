package com.x0550.rubiccubesolver.presentation.viewmodel

import com.x0550.rubiccubesolver.domain.model.CubeMove

data class CubeSolutionUiState(
    val definition: String,
    val moves: List<CubeMove>,
    val currentStepIndex: Int
)
