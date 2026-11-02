package com.x0550.rubiccubesolver

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.x0550.rubiccubesolver.domain.model.CubeMove
import com.x0550.rubiccubesolver.domain.model.Face
import com.x0550.rubiccubesolver.presentation.ui.CubeSolverActions
import com.x0550.rubiccubesolver.presentation.ui.CubeSolverVisualizer
import com.x0550.rubiccubesolver.presentation.viewmodel.CubeSolutionUiState
import com.x0550.rubiccubesolver.ui.theme.RubicCubeSolverTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SolutionExitTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var backDispatcher: OnBackPressedDispatcher
    private var resetCount = 0
    private var closeCount = 0
    private var nextCount = 0
    private var previousCount = 0
    private val move = CubeMove(Face.RIGHT, notation = "R")

    private fun text(id: Int) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun showSolution(moves: List<CubeMove>, index: Int) {
        compose.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            RubicCubeSolverTheme {
                CubeSolverVisualizer(
                    solution = CubeSolutionUiState(
                        definition = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB",
                        moves = moves,
                        currentStepIndex = index
                    ),
                    actions = CubeSolverActions(
                        next = { nextCount++ },
                        previous = { previousCount++ },
                        reset = { resetCount++ },
                        closeApp = { closeCount++ }
                    ),
                    snackbarHostState = androidx.compose.material3.SnackbarHostState()
                )
            }
        }
    }

    @Test
    fun alreadySolvedHasUsableActionsAndDismissKeepsScreen() {
        showSolution(emptyList(), 0)
        compose.onNodeWithText(text(R.string.solve_scan_cube)).assertIsEnabled()
        compose.onNodeWithText(text(R.string.solve_close_app)).performClick()
        compose.onNodeWithText(text(R.string.exit_solved_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.exit_stay_here)).performClick()
        compose.onNodeWithText(text(R.string.exit_solved_title)).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(0, resetCount)
            assertEquals(0, closeCount)
        }
        compose.onNodeWithText(text(R.string.solve_scan_cube)).performClick()
        compose.runOnIdle { assertEquals(1, resetCount) }
    }

    @Test
    fun finalStepBackShowsSolvedConfirmationAndOnlyConfirmCloses() {
        showSolution(listOf(move), 1)
        compose.onNodeWithText(text(R.string.solve_scan_cube)).assertIsEnabled()
        compose.onNodeWithText(text(R.string.solve_close_app)).assertIsEnabled()
        compose.runOnUiThread { backDispatcher.onBackPressed() }
        compose.onNodeWithText(text(R.string.exit_solved_header)).assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, closeCount) }
        compose.onNodeWithText(text(R.string.solve_close_app)).performClick()
        compose.runOnIdle {
            assertEquals(1, closeCount)
            assertEquals(0, resetCount)
        }
    }

    @Test
    fun midSolveBackWarnsAndDismissPreservesControls() {
        showSolution(listOf(move, move), 1)
        compose.runOnUiThread { backDispatcher.onBackPressed() }
        compose.onNodeWithText(text(R.string.exit_paused_body)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.exit_keep_solving)).performClick()
        compose.onNodeWithText(text(R.string.exit_paused_body)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.solve_replay)).assertIsEnabled().performClick()
        compose.onNodeWithContentDescription(text(R.string.solve_previous_move)).performClick()
        compose.onNodeWithText(text(R.string.solve_next)).performClick()
        compose.runOnIdle {
            assertEquals(1, previousCount)
            assertEquals(1, nextCount)
            assertEquals(0, resetCount)
            assertEquals(0, closeCount)
        }
        compose.runOnUiThread { backDispatcher.onBackPressed() }
        compose.onNodeWithText(text(R.string.solve_close_app)).performClick()
        compose.runOnIdle { assertEquals(1, closeCount) }
    }
}
