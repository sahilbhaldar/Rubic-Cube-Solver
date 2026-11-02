package com.x0550.rubiccubesolver.domain.model

import com.x0550.rubiccubesolver.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CubeDomainModelsTest {
    @Test
    fun everySolverSymbolMapsToItsColorAndUnknownSymbolsStayUnknown() {
        val expected = mapOf(
            'U' to CubeColor.WHITE,
            'R' to CubeColor.RED,
            'F' to CubeColor.GREEN,
            'D' to CubeColor.YELLOW,
            'L' to CubeColor.ORANGE,
            'B' to CubeColor.BLUE
        )

        expected.forEach { (symbol, color) ->
            assertEquals(color, CubeColor.fromChar(symbol))
            assertEquals(symbol, color.symbol)
        }
        assertEquals(CubeColor.UNKNOWN, CubeColor.fromChar('?'))
        assertEquals('?', CubeColor.UNKNOWN.symbol)
        assertEquals(R.string.color_unknown, CubeColor.UNKNOWN.nameRes)
    }

    @Test
    fun faceCentersMatchStandardCubeColors() {
        assertEquals(CubeColor.WHITE, Face.UP.centerColor)
        assertEquals(CubeColor.RED, Face.RIGHT.centerColor)
        assertEquals(CubeColor.GREEN, Face.FRONT.centerColor)
        assertEquals(CubeColor.YELLOW, Face.DOWN.centerColor)
        assertEquals(CubeColor.ORANGE, Face.LEFT.centerColor)
        assertEquals(CubeColor.BLUE, Face.BACK.centerColor)
    }

    @Test
    fun cubeMovesSupportValueEqualityCopyAndDefaults() {
        val original = CubeMove(Face.BACK, clockwise = false, double = true, notation = "B2")
        val copy = original.copy()

        assertEquals(original, copy)
        assertNotEquals(original, original.copy(clockwise = true))
        assertEquals(Face.BACK, original.face)
        assertEquals("B2", original.notation)
        assertEquals(true, original.double)
        assertEquals(CubeMove(Face.UP, clockwise = true, double = false, notation = ""), CubeMove(Face.UP))
    }

    @Test
    fun localizedErrorsPreserveResourceAndFormatArguments() {
        val args = listOf(CubeColor.BLUE, 4)
        val error = LocalizedError(R.string.solver_invalid_facelet_count, args)

        assertEquals(R.string.solver_invalid_facelet_count, error.messageRes)
        assertEquals(args, error.formatArgs)
        assertEquals(emptyList<Any>(), LocalizedError(R.string.solver_timeout).formatArgs)
    }

    @Test
    fun scanOrderReordersCapturedSidesToSolverFaceletOrder() {
        val captured = CubeScanOrder.faces.associateWith { face ->
            List(9) { face.centerColor }
        }

        assertEquals("UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB",
            CubeScanOrder.toSolverDefinition(captured))
        assertEquals(
            listOf(Face.FRONT, Face.RIGHT, Face.BACK, Face.LEFT, Face.UP, Face.DOWN),
            CubeScanOrder.faces
        )
    }

    @Test
    fun scanOrderRejectsMissingSidesAndIncorrectFaceSizes() {
        val missing = assertLocalizedError(R.string.scan_missing_side) {
            CubeScanOrder.toSolverDefinition(emptyMap())
        }
        assertEquals(listOf(Face.UP.centerColor), missing.formatArgs)

        val incomplete = CubeScanOrder.faces.associateWith { face ->
            List(9) { face.centerColor }
        }
            .toMutableMap()
            .apply { this[Face.UP] = List(8) { CubeColor.WHITE } }
        assertLocalizedError(R.string.scan_invalid_side_size) {
            CubeScanOrder.toSolverDefinition(incomplete)
        }
    }

    private fun assertLocalizedError(
        expectedResource: Int,
        action: () -> Unit
    ): LocalizedError {
        try {
            action()
        } catch (error: LocalizedError) {
            assertEquals(expectedResource, error.messageRes)
            return error
        }
        throw AssertionError("Expected LocalizedError with resource $expectedResource")
    }
}
