package com.x0550.rubiccubesolver.domain.engine

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineTwoPhaseSolverTest {
    @Test
    fun warmUpAndSolvedInputAreSafeAndReturnNoMoves() = runBlocking {
        val solver = OfflineTwoPhaseSolver()
        solver.warmUp()

        val result = solver.solveCube(SOLVED_CUBE).getOrThrow()

        assertTrue(result.isEmpty())
    }

    private companion object {
        const val SOLVED_CUBE = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB"
    }
}
