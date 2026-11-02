package com.x0550.rubiccubesolver.presentation.ui.cube3d

import com.x0550.rubiccubesolver.domain.model.CubeColor
import com.x0550.rubiccubesolver.domain.model.Face

/** Integer 3D vector. Axes: +x = Right, +y = Up, +z = Front. */
data class IVec(val x: Int, val y: Int, val z: Int) {
    operator fun plus(o: IVec) = IVec(x + o.x, y + o.y, z + o.z)
    operator fun unaryMinus() = IVec(-x, -y, -z)
    operator fun times(k: Int) = IVec(x * k, y * k, z * k)
    infix fun dot(o: IVec) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: IVec) = IVec(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)

    /** Rotates 90° clockwise as seen from the tip of [axis] looking back at the origin. */
    fun rotateClockwise(axis: IVec): IVec = -(axis cross this) + axis * (axis dot this)
}

val Face.normal: IVec
    get() = when (this) {
        Face.UP -> IVec(0, 1, 0)
        Face.RIGHT -> IVec(1, 0, 0)
        Face.FRONT -> IVec(0, 0, 1)
        Face.DOWN -> IVec(0, -1, 0)
        Face.LEFT -> IVec(-1, 0, 0)
        Face.BACK -> IVec(0, 0, -1)
    }

/** One of the 26 visible little cubes: its grid position and the colored stickers keyed by outward normal. */
data class Cubie(val pos: IVec, val stickers: Map<IVec, CubeColor>)

/** Sticker-level cube model used for 3D display; built from the same 54-facelet string as the solver. */
class CubeModel3D private constructor(val cubies: List<Cubie>) {

    /** Returns a new model with [face] turned [quarterTurns] times clockwise (1, 2 or 3). */
    fun turn(face: Face, quarterTurns: Int): CubeModel3D {
        val n = face.normal
        val turns = ((quarterTurns % 4) + 4) % 4
        return CubeModel3D(cubies.map { c ->
            if (c.pos dot n != 1) return@map c
            var pos = c.pos
            var stickers = c.stickers
            repeat(turns) {
                pos = pos.rotateClockwise(n)
                stickers = stickers.mapKeys { (k, _) -> k.rotateClockwise(n) }
            }
            Cubie(pos, stickers)
        })
    }

    fun toFacelets(): String {
        val byPos = cubies.associateBy { it.pos }
        val sb = StringBuilder(54)
        for (face in FACE_ORDER) for (r in 0 until 3) for (c in 0 until 3) {
            val color = byPos.getValue(faceletPosition(face, r, c)).stickers.getValue(face.normal)
            sb.append(color.symbol)
        }
        return sb.toString()
    }

    fun isSolved(): Boolean = FACE_ORDER.all { face ->
        cubies.filter { face.normal in it.stickers }.map { it.stickers.getValue(face.normal) }.distinct().size == 1
    }

    companion object {
        /** Kociemba facelet order. */
        val FACE_ORDER = listOf(Face.UP, Face.RIGHT, Face.FRONT, Face.DOWN, Face.LEFT, Face.BACK)

        /**
         * 3D position of sticker (row, col) of a face as laid out in the standard net / as scanned:
         * U seen from above with F at the bottom, D seen from below with F at the top,
         * side faces seen head-on with U at the top.
         */
        fun faceletPosition(face: Face, r: Int, c: Int): IVec = when (face) {
            Face.UP -> IVec(c - 1, 1, r - 1)
            Face.RIGHT -> IVec(1, 1 - r, 1 - c)
            Face.FRONT -> IVec(c - 1, 1 - r, 1)
            Face.DOWN -> IVec(c - 1, -1, 1 - r)
            Face.LEFT -> IVec(-1, 1 - r, c - 1)
            Face.BACK -> IVec(1 - c, 1 - r, -1)
        }

        fun fromFacelets(def: String): CubeModel3D {
            require(def.length == 54)
            val stickers = HashMap<IVec, MutableMap<IVec, CubeColor>>()
            FACE_ORDER.forEachIndexed { fi, face ->
                for (r in 0 until 3) for (c in 0 until 3) {
                    val pos = faceletPosition(face, r, c)
                    stickers.getOrPut(pos) { HashMap() }[face.normal] = CubeColor.fromChar(def[fi * 9 + r * 3 + c])
                }
            }
            return CubeModel3D(stickers.map { (pos, s) -> Cubie(pos, s) })
        }
    }
}