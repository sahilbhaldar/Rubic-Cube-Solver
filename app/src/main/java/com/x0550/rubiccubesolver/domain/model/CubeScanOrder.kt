package com.x0550.rubiccubesolver.domain.model

import com.x0550.rubiccubesolver.R

object CubeScanOrder {
    val faces = listOf(Face.FRONT, Face.RIGHT, Face.BACK, Face.LEFT, Face.UP, Face.DOWN)

    private val solverFaces = listOf(Face.UP, Face.RIGHT, Face.FRONT, Face.DOWN, Face.LEFT, Face.BACK)

    fun toSolverDefinition(capturedFaces: Map<Face, List<CubeColor>>): String = buildString {
        for (face in solverFaces) {
            val colors = capturedFaces[face]
                ?: throw LocalizedError(R.string.scan_missing_side, listOf(face.centerColor))
            if (colors.size != 9) throw LocalizedError(R.string.scan_invalid_side_size)
            colors.forEach { append(it.symbol) }
        }
    }
}