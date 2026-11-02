package com.x0550.rubiccubesolver.presentation.ui

data class CubeSolverActions(
    val next: () -> Unit,
    val previous: () -> Unit,
    val reset: () -> Unit,
    val closeApp: () -> Unit
)
