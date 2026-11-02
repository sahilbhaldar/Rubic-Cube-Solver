package com.x0550.rubiccubesolver

import com.x0550.rubiccubesolver.domain.engine.CubieCube
import com.x0550.rubiccubesolver.domain.engine.OfflineTwoPhaseSolver
import com.x0550.rubiccubesolver.domain.engine.TwoPhaseSolver
import com.x0550.rubiccubesolver.domain.model.CubeColor
import com.x0550.rubiccubesolver.domain.model.CubeScanOrder
import com.x0550.rubiccubesolver.domain.model.Face
import com.x0550.rubiccubesolver.presentation.ui.cube3d.CubeModel3D
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CubeSolverTest {

    private val solved = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB"
    private val faces = listOf(Face.UP, Face.RIGHT, Face.FRONT, Face.DOWN, Face.LEFT, Face.BACK)

    @Test
    fun solvedCubeRoundTrips() {
        assertEquals(solved, CubieCube().toFacelets())
        assertEquals(solved, CubeModel3D.fromFacelets(solved).toFacelets())
    }

    @Test
    fun modelMovesMatchCubieMoves() {
        for (m in 0 until 18) {
            val expected = CubieCube().multiply(CubieCube.MOVES[m]).toFacelets()
            val actual = CubeModel3D.fromFacelets(solved).turn(faces[m / 3], m % 3 + 1).toFacelets()
            assertEquals("move $m", expected, actual)
        }
    }

    @Test
    fun scannedModelMatchesEveryStepOfScramble() {
        val random = Random(73)
        var cube = CubieCube()
        var model = CubeModel3D.fromFacelets(solved)
        repeat(100) { step ->
            val move = random.nextInt(18)
            cube = cube.multiply(CubieCube.MOVES[move])
            model = model.turn(faces[move / 3], move % 3 + 1)
            assertEquals("step $step", cube.toFacelets(), model.toFacelets())
            assertEquals(cube.toFacelets(), CubeModel3D.fromFacelets(cube.toFacelets()).toFacelets())
        }
    }

    @Test
    fun alreadySolvedCubeNeedsNoMoves() = runBlocking {
        assertTrue(OfflineTwoPhaseSolver().solveCube(solved).getOrThrow().isEmpty())
    }

    @Test
    fun scanOrderVisitsFourSidesBeforeTopAndBottom() {
        assertEquals(
            listOf(Face.FRONT, Face.RIGHT, Face.BACK, Face.LEFT, Face.UP, Face.DOWN),
            CubeScanOrder.faces
        )
    }

    @Test
    fun sideFirstCapturePreservesScrambledSolverDefinition() = runBlocking {
        var cube = CubieCube()
        for (move in listOf(3, 0, 7, 16, 12, 9)) {
            cube = cube.multiply(CubieCube.MOVES[move])
        }
        val definition = cube.toFacelets()
        val captured = CubeScanOrder.faces.associateWith { face ->
            val offset = faces.indexOf(face) * 9
            definition.substring(offset, offset + 9).map(CubeColor::fromChar)
        }
        val reordered = CubeScanOrder.toSolverDefinition(captured)
        assertEquals(definition, reordered)
        val moves = OfflineTwoPhaseSolver().solveCube(reordered).getOrThrow()
        val result = moves.fold(CubeModel3D.fromFacelets(reordered)) { model, move ->
            model.turn(move.face, if (move.double) 2 else if (move.clockwise) 1 else 3)
        }
        assertTrue(result.isSolved())
    }

    @Test
    fun solvesRandomScrambles() = runBlocking {
        val rnd = Random(42)
        val solver = OfflineTwoPhaseSolver()
        repeat(20) {
            var cube = CubieCube()
            repeat(30) { cube = cube.multiply(CubieCube.MOVES[rnd.nextInt(18)]) }
            val def = cube.toFacelets()

            val moves = solver.solveCube(def).getOrThrow()
            assertTrue("too long: ${moves.size}", moves.size <= 24)

            var model = CubeModel3D.fromFacelets(def)
            for (mv in moves) model = model.turn(mv.face, if (mv.double) 2 else if (mv.clockwise) 1 else 3)
            assertTrue("not solved by ${moves.joinToString(" ") { it.notation }}", model.isSolved())
        }
    }

    @Test
    fun rejectsImpossibleCube() {
        // Swap two stickers of one edge (UF): edge flipped.
        val chars = solved.toCharArray()
        chars[7] = 'F'; chars[19] = 'U'
        val result = runCatching { CubieCube.fromFacelets(String(chars)) }
        assertTrue(result.isFailure)
    }

    @Test
    fun warmUpIsReasonablyFast() {
        val t = System.currentTimeMillis()
        TwoPhaseSolver.warmUp()
        println("table build ms: ${System.currentTimeMillis() - t}")
    }
}
