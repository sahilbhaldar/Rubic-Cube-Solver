package com.x0550.rubiccubesolver

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.x0550.rubiccubesolver.presentation.ui.CubicExitConfirmationDialog
import com.x0550.rubiccubesolver.presentation.ui.ScanInstructionSheet
import com.x0550.rubiccubesolver.ui.theme.RubicCubeSolverTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UiComponentsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun instructionsShowAllStepsAndStartDismissesSheet() {
        var dismissCount = 0
        compose.setContent {
            var visible by remember { mutableStateOf(true) }
            RubicCubeSolverTheme {
                if (visible) {
                    ScanInstructionSheet {
                        dismissCount++
                        visible = false
                    }
                }
            }
        }

        listOf(
            R.string.scan_instructions_title,
            R.string.scan_instructions_intro,
            R.string.scan_instructions_step_one,
            R.string.scan_instructions_step_two,
            R.string.scan_instructions_step_three
        ).forEach { resource ->
            compose.onNodeWithText(text(resource)).assertIsDisplayed()
        }
        compose.onNodeWithText(text(R.string.scan_instructions_start)).performClick()
        compose.onNodeWithText(text(R.string.scan_instructions_title)).assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, dismissCount) }
    }

    @Test
    fun pausedDialogDismissDoesNotConfirmExit() {
        var dismissCount = 0
        var confirmCount = 0
        compose.setContent {
            RubicCubeSolverTheme {
                CubicExitConfirmationDialog(
                    solved = false,
                    onDismiss = { dismissCount++ },
                    onConfirm = { confirmCount++ }
                )
            }
        }

        compose.onNodeWithText(text(R.string.exit_paused_header)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.exit_paused_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.exit_paused_body)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.exit_keep_solving)).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, dismissCount)
            assertEquals(0, confirmCount)
        }
    }

    @Test
    fun solvedDialogConfirmDoesNotDismiss() {
        var dismissCount = 0
        var confirmCount = 0
        compose.setContent {
            RubicCubeSolverTheme {
                CubicExitConfirmationDialog(
                    solved = true,
                    onDismiss = { dismissCount++ },
                    onConfirm = { confirmCount++ }
                )
            }
        }

        compose.onNodeWithText(text(R.string.exit_solved_header)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.exit_solved_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.exit_solved_body)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.solve_close_app)).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(0, dismissCount)
            assertEquals(1, confirmCount)
        }
    }

    private fun text(resource: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resource)
}
