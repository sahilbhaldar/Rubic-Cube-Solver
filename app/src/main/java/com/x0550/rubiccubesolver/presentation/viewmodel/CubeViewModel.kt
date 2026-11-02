package com.x0550.rubiccubesolver.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.x0550.rubiccubesolver.R
import com.x0550.rubiccubesolver.data.analyzer.CubeFrameAnalyzer
import com.x0550.rubiccubesolver.domain.engine.CubeSolverRepository
import com.x0550.rubiccubesolver.domain.model.CubeColor
import com.x0550.rubiccubesolver.domain.model.CubeScanOrder
import com.x0550.rubiccubesolver.domain.model.Face
import com.x0550.rubiccubesolver.domain.model.LocalizedError
import com.x0550.rubiccubesolver.domain.repository.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CubeViewModel @Inject constructor(
    private val analyzer: CubeFrameAnalyzer,
    private val solverRepository: CubeSolverRepository,
    private val onboardingRepository: OnboardingRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CubeScanUiState())
    val uiState: StateFlow<CubeScanUiState> = _uiState.asStateFlow()

    val facesOrder = CubeScanOrder.faces

    init {
        analyzer.setExpectedCenter(facesOrder.first().centerColor)
        viewModelScope.launch { solverRepository.warmUp() }
        viewModelScope.launch {
            onboardingRepository.hasSeenScanInstructions().collect { hasSeen ->
                _uiState.update { it.copy(showScanInstructionSheet = !hasSeen) }
            }
        }
        viewModelScope.launch {
            analyzer.detectedColors.collect { colors ->
                _uiState.update { it.copy(currentFrameColors = colors) }
            }
        }
    }

    fun getAnalyzer(): CubeFrameAnalyzer = analyzer

    fun dismissScanInstructions() {
        viewModelScope.launch {
            onboardingRepository.markScanInstructionsSeen()
            _uiState.update { it.copy(showScanInstructionSheet = false) }
        }
    }

    /** Freeze the current detection for review; the user can fix stickers, then confirm or recapture. */
    fun captureCurrentFace() {
        val currentState = _uiState.value
        if (currentState.currentFaceIndex >= facesOrder.size) return
        val face = facesOrder[currentState.currentFaceIndex]
        val currentColors = currentState.currentFrameColors.toMutableList().apply {
            // The center sticker never moves, so it is always the face's own color.
            this[4] = face.centerColor
        }
        _uiState.update { it.copy(pendingFaceColors = currentColors) }
    }

    /** Cycle a sticker of the face under review to the next color (center is fixed). */
    fun cycleStickerColor(index: Int) {
        if (index == 4) return
        _uiState.update { state ->
            val pending = state.pendingFaceColors ?: return@update state
            val choices = CubeColor.entries.filter { it != CubeColor.UNKNOWN }
            val next = choices[(choices.indexOf(pending[index]) + 1) % choices.size]
            state.copy(pendingFaceColors = pending.toMutableList().apply { this[index] = next })
        }
    }

    fun recaptureFace() {
        _uiState.update { it.copy(pendingFaceColors = null) }
    }

    fun updateCapturedFace(face: Face, colors: List<CubeColor>) {
        if (colors.size != 9 || colors.any { it == CubeColor.UNKNOWN }) {
            throw LocalizedError(R.string.scan_invalid_capture)
        }
        if (colors[4] != face.centerColor) {
            throw LocalizedError(R.string.scan_fixed_center)
        }
        _uiState.update { state ->
            if (state.isSolving || face !in state.capturedFaces) state
            else state.copy(capturedFaces = state.capturedFaces + (face to colors.toList()), errorMessage = null)
        }
    }

    fun reviewCapturedFaces() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun retrySolve() {
        val state = _uiState.value
        if (!state.isSolving && state.capturedFaces.size == facesOrder.size) {
            solveCube(state.capturedFaces)
        }
    }

    fun confirmFace() {
        val currentState = _uiState.value
        val pending = currentState.pendingFaceColors ?: return
        val face = facesOrder[currentState.currentFaceIndex]
        analyzer.calibrateFromCenter(face.centerColor)

        val updated = currentState.capturedFaces.toMutableMap()
        updated[face] = pending

        if (currentState.currentFaceIndex < facesOrder.size - 1) {
            analyzer.setExpectedCenter(facesOrder[currentState.currentFaceIndex + 1].centerColor)
            _uiState.update {
                it.copy(
                    capturedFaces = updated,
                    currentFaceIndex = it.currentFaceIndex + 1,
                    pendingFaceColors = null
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    capturedFaces = updated,
                    currentFaceIndex = facesOrder.size,
                    pendingFaceColors = null
                )
            }
            solveCube(updated)
        }
    }

    private fun solveCube(faces: Map<Face, List<CubeColor>>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSolving = true, errorMessage = null) }

            val counts = faces.values.flatten().groupingBy { it }.eachCount()
            val wrong = CubeColor.entries
                .filter { it != CubeColor.UNKNOWN && (counts[it] ?: 0) != 9 }
                .map { context.getString(R.string.scan_color_count_item, context.getString(it.nameRes), counts[it] ?: 0) }
            if (wrong.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        isSolving = false,
                        errorMessage = context.getString(R.string.scan_color_count_error, wrong.joinToString(", "))
                    )
                }
                return@launch
            }
            val definition = try {
                CubeScanOrder.toSolverDefinition(faces)
            } catch (error: LocalizedError) {
                _uiState.update {
                    it.copy(isSolving = false, errorMessage = localizedMessage(error))
                }
                return@launch
            }

            val result = solverRepository.solveCube(definition)
            result.onSuccess { moves ->
                _uiState.update {
                    it.copy(
                        isSolving = false,
                        moves = moves,
                        currentStepIndex = 0,
                        scannedDefinition = definition,
                        solutionReady = true
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSolving = false,
                        errorMessage = if (error is LocalizedError) localizedMessage(error)
                            else context.getString(R.string.solver_internal_error)
                    )
                }
            }
        }
    }

    private fun localizedMessage(error: LocalizedError): String {
        val args = error.formatArgs.map { value ->
            if (value is CubeColor) context.getString(value.nameRes) else value
        }.toTypedArray()
        return context.getString(error.messageRes, *args)
    }

    /** Step index runs 0..moves.size; moves.size means "all moves done, cube solved". */
    fun nextStep() {
        _uiState.update {
            if (it.currentStepIndex < it.moves.size) {
                it.copy(currentStepIndex = it.currentStepIndex + 1)
            } else it
        }
    }

    fun previousStep() {
        _uiState.update {
            if (it.currentStepIndex > 0) {
                it.copy(currentStepIndex = it.currentStepIndex - 1)
            } else it
        }
    }

    fun reset() {
        analyzer.resetCalibration()
        analyzer.setExpectedCenter(facesOrder.first().centerColor)
        _uiState.value = CubeScanUiState()
    }
}