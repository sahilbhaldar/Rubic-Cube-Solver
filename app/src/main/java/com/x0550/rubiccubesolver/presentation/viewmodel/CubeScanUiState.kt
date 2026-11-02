package com.x0550.rubiccubesolver.presentation.viewmodel

import com.x0550.rubiccubesolver.domain.model.CubeColor
import com.x0550.rubiccubesolver.domain.model.CubeMove
import com.x0550.rubiccubesolver.domain.model.Face

data class CubeScanUiState(
    val currentFaceIndex: Int = 0,
    val capturedFaces: Map<Face, List<CubeColor>> = emptyMap(),
    val currentFrameColors: List<CubeColor> = List(9) { CubeColor.UNKNOWN },
    /** Non-null while the user is reviewing/correcting a captured face before confirming it. */
    val pendingFaceColors: List<CubeColor>? = null,
    val isSolving: Boolean = false,
    /** 54-char facelet string of the scanned cube, set once all 6 faces are confirmed. */
    val scannedDefinition: String? = null,
    /** True once the solver has produced a (possibly empty) solution for [scannedDefinition]. */
    val solutionReady: Boolean = false,
    val moves: List<CubeMove> = emptyList(),
    val currentStepIndex: Int = 0,
    val errorMessage: String? = null,
    val showScanInstructionSheet: Boolean = false
)