package com.x0550.rubiccubesolver.domain.engine

import com.x0550.rubiccubesolver.R
import com.x0550.rubiccubesolver.domain.model.CubeColor
import com.x0550.rubiccubesolver.domain.model.CubeMove
import com.x0550.rubiccubesolver.domain.model.Face
import com.x0550.rubiccubesolver.domain.model.LocalizedError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface CubeSolverRepository {
    suspend fun solveCube(scrambledFacelets: String): Result<List<CubeMove>>

    /** Pre-builds solver tables so the first solve is quick. */
    suspend fun warmUp() {}
}

@Singleton
class OfflineTwoPhaseSolver @Inject constructor() : CubeSolverRepository {

    override suspend fun warmUp() = withContext(Dispatchers.Default) { TwoPhaseSolver.warmUp() }

    /**
     * Solves the cube using Kociemba standard 54-facelet representation:
     * U1-U9, R1-R9, F1-F9, D1-D9, L1-L9, B1-B9
     */
    override suspend fun solveCube(scrambledFacelets: String): Result<List<CubeMove>> = withContext(Dispatchers.Default) {
        runCatching {
            validateCube(scrambledFacelets)
            val movesStr = computeKociembaSolution(scrambledFacelets)
            parseMoves(movesStr)
        }
    }

    private fun validateCube(definition: String) {
        if (definition.length != 54) throw LocalizedError(R.string.solver_wrong_definition_length)
        val counts = definition.groupingBy { it }.eachCount()
        for (char in "URFDLB") {
            val count = counts[char] ?: 0
            if (count != 9) {
                throw LocalizedError(
                    R.string.solver_invalid_facelet_count,
                    listOf(CubeColor.fromChar(char), count)
                )
            }
        }
    }

    private fun computeKociembaSolution(definition: String): String {
        val cube = CubieCube.fromFacelets(definition)
        val moves = TwoPhaseSolver.solve(cube)
        return moves.joinToString(" ") { m ->
            CubieCube.FACE_CHARS[m / 3] + when (m % 3) {
                0 -> ""
                1 -> "2"
                else -> "'"
            }
        }
    }

    private fun parseMoves(moveStr: String): List<CubeMove> {
        if (moveStr.isBlank()) return emptyList()
        return moveStr.split("\\s+".toRegex()).mapNotNull { token ->
            if (token.isEmpty()) return@mapNotNull null
            val faceChar = token[0]
            val face = when (faceChar) {
                'U' -> Face.UP
                'R' -> Face.RIGHT
                'F' -> Face.FRONT
                'D' -> Face.DOWN
                'L' -> Face.LEFT
                'B' -> Face.BACK
                else -> return@mapNotNull null
            }
            val isDouble = token.contains("2")
            val isCounter = token.contains("'")
            CubeMove(
                face = face,
                clockwise = !isCounter,
                double = isDouble,
                notation = token
            )
        }
    }
}