package com.x0550.rubiccubesolver.domain.engine

import com.x0550.rubiccubesolver.R
import com.x0550.rubiccubesolver.domain.model.CubeColor
import com.x0550.rubiccubesolver.domain.model.LocalizedError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CubeSolverValidationTest {
    private val solved = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB"

    @Test
    fun solverReportsIncorrectDefinitionLengthAndColorCounts() = runBlocking {
        val solver = OfflineTwoPhaseSolver()
        assertSolverError(R.string.solver_wrong_definition_length) {
            solver.solveCube(solved.dropLast(1))
        }

        val incorrectCounts = solved.toCharArray().apply { this[0] = 'R' }.concatToString()
        val countError = assertSolverError(R.string.solver_invalid_facelet_count) {
            solver.solveCube(incorrectCounts)
        }
        assertEquals(listOf(CubeColor.WHITE, 8), countError.formatArgs)
    }

    @Test
    fun parserRejectsUnknownColorsAndImpossibleCornerOrEdgePieces() {
        val unknownColor = solved.toCharArray().apply { this[0] = '?' }.concatToString()
        assertCubieError(R.string.solver_unknown_color, unknownColor)

        val impossibleCorner = solved.toCharArray().apply { this[20] = 'D' }.concatToString()
        assertCubieError(R.string.solver_impossible_corner, impossibleCorner)

        val impossibleEdge = solved.toCharArray().apply { this[10] = 'D' }.concatToString()
        assertCubieError(R.string.solver_impossible_edge, impossibleEdge)
    }

    @Test
    fun parserRejectsDuplicateAndInvalidlyOrientedPieces() {
        val duplicateCorner = solved.toCharArray().apply { this[20] = 'B' }.concatToString()
        assertCubieError(R.string.solver_duplicate_corner, duplicateCorner)

        val twistedCorner = solved.toCharArray().apply {
            this[8] = 'R'
            this[9] = 'F'
            this[20] = 'U'
        }.concatToString()
        assertCubieError(R.string.solver_twisted_corner, twistedCorner)

        val duplicateEdge = solved.toCharArray().apply { this[10] = 'F' }.concatToString()
        assertCubieError(R.string.solver_duplicate_edge, duplicateEdge)

        val flippedEdge = solved.toCharArray().apply {
            this[7] = 'F'
            this[19] = 'U'
        }.concatToString()
        assertCubieError(R.string.solver_flipped_edge, flippedEdge)
    }

    @Test
    fun parserRejectsSwappedPiecesWithDifferentPermutationParity() {
        val invalidCube = CubieCube().apply {
            val first = ep[0]
            ep[0] = ep[1]
            ep[1] = first
        }.toFacelets()

        assertCubieError(R.string.solver_swapped_pieces, invalidCube)
    }

    private suspend fun assertSolverError(
        expectedResource: Int,
        action: suspend () -> Result<List<com.x0550.rubiccubesolver.domain.model.CubeMove>>
    ): LocalizedError {
        val error = action().exceptionOrNull()
        assertNotNull(error)
        assertEquals(expectedResource, (error as LocalizedError).messageRes)
        return error
    }

    private fun assertCubieError(expectedResource: Int, facelets: String) {
        try {
            CubieCube.fromFacelets(facelets)
        } catch (error: LocalizedError) {
            assertEquals(expectedResource, error.messageRes)
            return
        }
        throw AssertionError("Expected LocalizedError with resource $expectedResource")
    }
}
