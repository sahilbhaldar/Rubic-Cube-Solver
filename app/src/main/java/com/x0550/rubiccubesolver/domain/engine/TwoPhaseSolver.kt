package com.x0550.rubiccubesolver.domain.engine

import com.x0550.rubiccubesolver.R
import com.x0550.rubiccubesolver.domain.model.CubeColor
import com.x0550.rubiccubesolver.domain.model.LocalizedError
import kotlin.math.max
import kotlin.math.min

/**
 * Cubie-level cube: permutation + orientation of the 8 corners and 12 edges (Kociemba conventions).
 * Corners: URF, UFL, ULB, UBR, DFR, DLF, DBL, DRB. Edges: UR, UF, UL, UB, DR, DF, DL, DB, FR, FL, BL, BR.
 */
internal class CubieCube(
    val cp: IntArray = IntArray(8) { it },
    val co: IntArray = IntArray(8),
    val ep: IntArray = IntArray(12) { it },
    val eo: IntArray = IntArray(12)
) {
    fun multiply(b: CubieCube): CubieCube {
        val r = CubieCube()
        for (i in 0 until 8) {
            r.cp[i] = cp[b.cp[i]]
            r.co[i] = (co[b.cp[i]] + b.co[i]) % 3
        }
        for (i in 0 until 12) {
            r.ep[i] = ep[b.ep[i]]
            r.eo[i] = (eo[b.ep[i]] + b.eo[i]) % 2
        }
        return r
    }

    fun twist(): Int {
        var t = 0
        for (i in 0 until 7) t = t * 3 + co[i]
        return t
    }

    fun flip(): Int {
        var f = 0
        for (i in 0 until 11) f = f * 2 + eo[i]
        return f
    }

    /** Which 4 positions hold the UD-slice edges (FR, FL, BL, BR), ranked 0..494. */
    fun slice(): Int {
        var r = 0
        var k = 0
        for (pos in 0 until 12) {
            if (ep[pos] >= 8) {
                k++
                r += binomial(pos, k)
            }
        }
        return r
    }

    fun cornerPerm(): Int = permRank(cp, 0, 8)
    fun udEdgePerm(): Int = permRank(ep, 0, 8)
    fun slicePerm(): Int = permRank(ep, 8, 4)

    fun toFacelets(): String {
        val f = CharArray(54)
        for (i in 0 until 6) f[i * 9 + 4] = FACE_CHARS[i]
        for (i in 0 until 8) for (n in 0 until 3) {
            f[CORNER_FACELET[i][(n + co[i]) % 3]] = FACE_CHARS[CORNER_COLOR[cp[i]][n]]
        }
        for (i in 0 until 12) for (n in 0 until 2) {
            f[EDGE_FACELET[i][(n + eo[i]) % 2]] = FACE_CHARS[EDGE_COLOR[ep[i]][n]]
        }
        return String(f)
    }

    companion object {
        const val FACE_CHARS = "URFDLB"

        // Facelet indices: U 0..8, R 9..17, F 18..26, D 27..35, L 36..44, B 45..53.
        val CORNER_FACELET = arrayOf(
            intArrayOf(8, 9, 20), intArrayOf(6, 18, 38), intArrayOf(0, 36, 47), intArrayOf(2, 45, 11),
            intArrayOf(29, 26, 15), intArrayOf(27, 44, 24), intArrayOf(33, 53, 42), intArrayOf(35, 17, 51)
        )
        val EDGE_FACELET = arrayOf(
            intArrayOf(5, 10), intArrayOf(7, 19), intArrayOf(3, 37), intArrayOf(1, 46),
            intArrayOf(32, 16), intArrayOf(28, 25), intArrayOf(30, 43), intArrayOf(34, 52),
            intArrayOf(23, 12), intArrayOf(21, 41), intArrayOf(50, 39), intArrayOf(48, 14)
        )

        // Face indices: U0 R1 F2 D3 L4 B5.
        val CORNER_COLOR = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(0, 2, 4), intArrayOf(0, 4, 5), intArrayOf(0, 5, 1),
            intArrayOf(3, 2, 1), intArrayOf(3, 4, 2), intArrayOf(3, 5, 4), intArrayOf(3, 1, 5)
        )
        val EDGE_COLOR = arrayOf(
            intArrayOf(0, 1), intArrayOf(0, 2), intArrayOf(0, 4), intArrayOf(0, 5),
            intArrayOf(3, 1), intArrayOf(3, 2), intArrayOf(3, 4), intArrayOf(3, 5),
            intArrayOf(2, 1), intArrayOf(2, 4), intArrayOf(5, 4), intArrayOf(5, 1)
        )

        /** Clockwise quarter turns of U, R, F, D, L, B. */
        val BASIC_MOVES: Array<CubieCube> = arrayOf(
            CubieCube(intArrayOf(3, 0, 1, 2, 4, 5, 6, 7), IntArray(8),
                intArrayOf(3, 0, 1, 2, 4, 5, 6, 7, 8, 9, 10, 11), IntArray(12)),
            CubieCube(intArrayOf(4, 1, 2, 0, 7, 5, 6, 3), intArrayOf(2, 0, 0, 1, 1, 0, 0, 2),
                intArrayOf(8, 1, 2, 3, 11, 5, 6, 7, 4, 9, 10, 0), IntArray(12)),
            CubieCube(intArrayOf(1, 5, 2, 3, 0, 4, 6, 7), intArrayOf(1, 2, 0, 0, 2, 1, 0, 0),
                intArrayOf(0, 9, 2, 3, 4, 8, 6, 7, 1, 5, 10, 11), intArrayOf(0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0)),
            CubieCube(intArrayOf(0, 1, 2, 3, 5, 6, 7, 4), IntArray(8),
                intArrayOf(0, 1, 2, 3, 5, 6, 7, 4, 8, 9, 10, 11), IntArray(12)),
            CubieCube(intArrayOf(0, 2, 6, 3, 4, 1, 5, 7), intArrayOf(0, 1, 2, 0, 0, 2, 1, 0),
                intArrayOf(0, 1, 10, 3, 4, 5, 9, 7, 8, 2, 6, 11), IntArray(12)),
            CubieCube(intArrayOf(0, 1, 3, 7, 4, 5, 2, 6), intArrayOf(0, 0, 1, 2, 0, 0, 2, 1),
                intArrayOf(0, 1, 2, 11, 4, 5, 6, 10, 8, 9, 3, 7), intArrayOf(0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1))
        )

        /** All 18 moves; index = face * 3 + (quarterTurns - 1). */
        val MOVES: Array<CubieCube> = Array(18) { m ->
            var c = BASIC_MOVES[m / 3]
            repeat(m % 3) { c = c.multiply(BASIC_MOVES[m / 3]) }
            c
        }

        /** Parses a 54-char URFDLB facelet string, throwing a user-readable error if it isn't a real cube. */
        fun fromFacelets(def: String): CubieCube {
            val facelets = parseFacelets(def)
            val cc = CubieCube()
            populateCorners(facelets, cc)
            populateEdges(facelets, cc)
            validateCubeState(cc)
            return cc
        }

        private fun parseFacelets(definition: String): IntArray {
            if (definition.length != 54) {
                throw LocalizedError(R.string.solver_wrong_definition_length)
            }
            return IntArray(54) { index ->
                FACE_CHARS.indexOf(definition[index]).also { color ->
                    if (color < 0) throw LocalizedError(R.string.solver_unknown_color)
                }
            }
        }

        private fun populateCorners(facelets: IntArray, cube: CubieCube) {
            for (position in 0 until 8) {
                val orientation = (0 until 3).firstOrNull { offset ->
                    val color = facelets[CORNER_FACELET[position][offset]]
                    color == 0 || color == 3
                } ?: throw LocalizedError(
                    R.string.solver_corner_missing_ud,
                    listOf(CubeColor.WHITE, CubeColor.YELLOW)
                )
                val firstColor = facelets[CORNER_FACELET[position][(orientation + 1) % 3]]
                val secondColor = facelets[CORNER_FACELET[position][(orientation + 2) % 3]]
                val cubie = (0 until 8).firstOrNull { index ->
                    CORNER_COLOR[index][1] == firstColor && CORNER_COLOR[index][2] == secondColor
                } ?: throw LocalizedError(R.string.solver_impossible_corner)
                cube.cp[position] = cubie
                cube.co[position] = orientation
            }
        }

        private fun populateEdges(facelets: IntArray, cube: CubieCube) {
            for (position in 0 until 12) {
                val firstColor = facelets[EDGE_FACELET[position][0]]
                val secondColor = facelets[EDGE_FACELET[position][1]]
                val normal = (0 until 12).firstOrNull { index ->
                    EDGE_COLOR[index][0] == firstColor && EDGE_COLOR[index][1] == secondColor
                }
                val flipped = (0 until 12).firstOrNull { index ->
                    EDGE_COLOR[index][0] == secondColor && EDGE_COLOR[index][1] == firstColor
                }
                when {
                    normal != null -> {
                        cube.ep[position] = normal
                        cube.eo[position] = 0
                    }
                    flipped != null -> {
                        cube.ep[position] = flipped
                        cube.eo[position] = 1
                    }
                    else -> throw LocalizedError(R.string.solver_impossible_edge)
                }
            }
        }

        private fun validateCubeState(cube: CubieCube) {
            if (cube.cp.distinct().size != 8) throw LocalizedError(R.string.solver_duplicate_corner)
            if (cube.ep.distinct().size != 12) throw LocalizedError(R.string.solver_duplicate_edge)
            if (cube.co.sum() % 3 != 0) throw LocalizedError(R.string.solver_twisted_corner)
            if (cube.eo.sum() % 2 != 0) throw LocalizedError(R.string.solver_flipped_edge)
            if (parity(cube.cp) != parity(cube.ep)) throw LocalizedError(R.string.solver_swapped_pieces)
        }

        fun binomial(n: Int, k: Int): Int {
            if (k > n) return 0
            var r = 1
            for (i in 0 until k) r = r * (n - i) / (i + 1)
            return r
        }

        fun permRank(a: IntArray, off: Int, n: Int): Int {
            var r = 0
            for (i in 0 until n) {
                var smaller = 0
                for (j in i + 1 until n) if (a[off + j] < a[off + i]) smaller++
                r = r * (n - i) + smaller
            }
            return r
        }

        private fun parity(p: IntArray): Int {
            var s = 0
            for (i in p.indices) for (j in i + 1 until p.size) if (p[j] < p[i]) s++
            return s % 2
        }
    }
}

/**
 * Kociemba two-phase algorithm. Phase 1 brings the cube into G1 = <U, D, R2, L2, F2, B2> (orientations solved,
 * slice edges in the slice); phase 2 solves it using only G1 moves. Tables are built once (~1-2 s) and cached.
 */
internal object TwoPhaseSolver {
    private const val N_TWIST = 2187
    private const val N_FLIP = 2048
    private const val N_SLICE = 495
    private const val N_PERM8 = 40320
    private const val N_SLICE_PERM = 24
    private const val N_MOVES = 18
    private const val MAX_LENGTH = 30

    private val P2_MOVES = intArrayOf(0, 1, 2, 4, 7, 9, 10, 11, 13, 16)
    private val IS_P2_MOVE = BooleanArray(N_MOVES).also { a -> P2_MOVES.forEach { a[it] = true } }

    private class Tables {
        val twistMove = buildMoveTable(N_TWIST, { it.twist() }, IntArray(N_MOVES) { it })
        val flipMove = buildMoveTable(N_FLIP, { it.flip() }, IntArray(N_MOVES) { it })
        val sliceMove = buildMoveTable(N_SLICE, { it.slice() }, IntArray(N_MOVES) { it })
        val cpMove = buildMoveTable(N_PERM8, { it.cornerPerm() }, P2_MOVES)
        val udMove = buildMoveTable(N_PERM8, { it.udEdgePerm() }, P2_MOVES)
        val spMove = buildMoveTable(N_SLICE_PERM, { it.slicePerm() }, P2_MOVES)

        val sliceGoal = CubieCube().slice()

        val pruneTwistSlice = buildPrune(
            PruningTableConfig(
                PruningCoordinates(N_TWIST, N_SLICE),
                PruningMoves(twistMove, sliceMove, N_MOVES),
                PruningGoal(0, sliceGoal)
            )
        )
        val pruneFlipSlice = buildPrune(
            PruningTableConfig(
                PruningCoordinates(N_FLIP, N_SLICE),
                PruningMoves(flipMove, sliceMove, N_MOVES),
                PruningGoal(0, sliceGoal)
            )
        )
        val pruneCpSp = buildPrune(
            PruningTableConfig(
                PruningCoordinates(N_PERM8, N_SLICE_PERM),
                PruningMoves(cpMove, spMove, P2_MOVES.size),
                PruningGoal(0, 0)
            )
        )
        val pruneUdSp = buildPrune(
            PruningTableConfig(
                PruningCoordinates(N_PERM8, N_SLICE_PERM),
                PruningMoves(udMove, spMove, P2_MOVES.size),
                PruningGoal(0, 0)
            )
        )
    }

    private val tables by lazy { Tables() }

    /** Builds lookup tables ahead of time so the first solve is fast. */
    fun warmUp() {
        tables
    }

    /**
     * Returns a solution as a list of move indices (face * 3 + quarterTurns - 1).
     * Keeps improving the solution until [timeBudgetMs] elapses after the first solution is found.
     */
    fun solve(cube: CubieCube, timeBudgetMs: Long = 1500, hardLimitMs: Long = 20_000): IntArray {
        return Search(tables, cube, timeBudgetMs, hardLimitMs).run()
            ?: throw LocalizedError(R.string.solver_timeout)
    }

    private class Search(
        val t: Tables,
        val cube: CubieCube,
        timeBudgetMs: Long,
        hardLimitMs: Long
    ) {
        private val start = System.nanoTime()
        private val softDeadline = start + timeBudgetMs * 1_000_000
        private val hardDeadline = start + hardLimitMs * 1_000_000
        private val sol = IntArray(MAX_LENGTH + 1)
        private var best: IntArray? = null
        private var maxTotal = MAX_LENGTH
        private var nodes = 0L
        private var stop = false

        fun run(): IntArray? {
            val tw = cube.twist()
            val fl = cube.flip()
            val sl = cube.slice()
            val coordinates = packPhase1(tw, fl, sl)
            var d1 = 0
            while (d1 <= maxTotal && !stop) {
                phase1(coordinates, 0, d1, -1)
                d1++
            }
            return best
        }

        private fun packPhase1(twist: Int, flip: Int, slice: Int): Long =
            (twist.toLong() shl 20) or (flip.toLong() shl 9) or slice.toLong()

        private fun packPhase2(corner: Int, edge: Int, slice: Int): Long =
            (corner.toLong() shl 21) or (edge.toLong() shl 5) or slice.toLong()

        private fun timeUp(): Boolean {
            val now = System.nanoTime()
            return now > hardDeadline || (best != null && now > softDeadline)
        }

        private fun phase1(coordinates: Long, depth: Int, togo: Int, lastFace: Int) {
            val tw = (coordinates shr 20).toInt()
            val fl = ((coordinates shr 9) and 0x7FF).toInt()
            val sl = (coordinates and 0x1FF).toInt()
            if (stop) return
            if (togo == 0) {
                if (isPhase1Goal(tw, fl, sl, depth)) phase2Start(depth)
                return
            }
            if ((++nodes and 0xFFF) == 0L && timeUp()) { stop = true; return }
            searchPhase1Moves(coordinates, depth, togo, lastFace)
        }

        private fun isPhase1Goal(twist: Int, flip: Int, slice: Int, depth: Int): Boolean =
            twist == 0 && flip == 0 && slice == t.sliceGoal &&
                (depth == 0 || !IS_P2_MOVE[sol[depth - 1]])

        private fun searchPhase1Moves(coordinates: Long, depth: Int, togo: Int, lastFace: Int) {
            val twist = (coordinates shr 20).toInt()
            val flip = ((coordinates shr 9) and 0x7FF).toInt()
            val slice = (coordinates and 0x1FF).toInt()
            for (m in 0 until N_MOVES) {
                val face = m / 3
                if (face == lastFace || face == lastFace - 3) continue
                val nt = t.twistMove[twist * N_MOVES + m]
                val nf = t.flipMove[flip * N_MOVES + m]
                val ns = t.sliceMove[slice * N_MOVES + m]
                val h = max(
                    t.pruneTwistSlice[nt * N_SLICE + ns].toInt(),
                    t.pruneFlipSlice[nf * N_SLICE + ns].toInt()
                )
                if (h > togo - 1) continue
                sol[depth] = m
                phase1(packPhase1(nt, nf, ns), depth + 1, togo - 1, face)
                if (stop) return
            }
        }

        private fun phase2Start(d1: Int) {
            var c = cube
            for (i in 0 until d1) c = c.multiply(CubieCube.MOVES[sol[i]])
            val cp = c.cornerPerm()
            val ud = c.udEdgePerm()
            val sp = c.slicePerm()
            val h = max(
                t.pruneCpSp[cp * N_SLICE_PERM + sp].toInt(),
                t.pruneUdSp[ud * N_SLICE_PERM + sp].toInt()
            )
            val limit = min(maxTotal - d1, 18)
            val lastFace = if (d1 > 0) sol[d1 - 1] / 3 else -1
            val coordinates = packPhase2(cp, ud, sp)
            for (d2 in h..limit) {
                if (phase2(coordinates, d1, d2, lastFace)) {
                    best = sol.copyOf(d1 + d2)
                    maxTotal = d1 + d2 - 1
                    if (timeUp()) stop = true
                    return
                }
                if (stop) return
            }
        }

        private fun phase2(coordinates: Long, depth: Int, togo: Int, lastFace: Int): Boolean {
            val cp = (coordinates shr 21).toInt()
            val ud = ((coordinates shr 5) and 0xFFFF).toInt()
            val sp = (coordinates and 0x1F).toInt()
            if (togo == 0) return cp == 0 && ud == 0 && sp == 0
            if ((++nodes and 0xFFF) == 0L && System.nanoTime() > hardDeadline) { stop = true; return false }
            return searchPhase2Moves(coordinates, depth, togo, lastFace)
        }

        private fun searchPhase2Moves(coordinates: Long, depth: Int, togo: Int, lastFace: Int): Boolean {
            val cp = (coordinates shr 21).toInt()
            val ud = ((coordinates shr 5) and 0xFFFF).toInt()
            val sp = (coordinates and 0x1F).toInt()
            val n = P2_MOVES.size
            for (mi in 0 until n) {
                val m = P2_MOVES[mi]
                val face = m / 3
                if (face == lastFace || face == lastFace - 3) continue
                val ncp = t.cpMove[cp * n + mi]
                val nud = t.udMove[ud * n + mi]
                val nsp = t.spMove[sp * n + mi]
                val h = max(
                    t.pruneCpSp[ncp * N_SLICE_PERM + nsp].toInt(),
                    t.pruneUdSp[nud * N_SLICE_PERM + nsp].toInt()
                )
                if (h > togo - 1) continue
                sol[depth] = m
                if (phase2(packPhase2(ncp, nud, nsp), depth + 1, togo - 1, face)) return true
                if (stop) return false
            }
            return false
        }
    }

    /** BFS from solved over [moves]; coordinate must depend only on itself under those moves. */
    private fun buildMoveTable(size: Int, coord: (CubieCube) -> Int, moves: IntArray): IntArray {
        val k = moves.size
        val table = IntArray(size * k) { -1 }
        val reps = arrayOfNulls<CubieCube>(size)
        val queue = IntArray(size)
        var head = 0
        var tail = 0
        val solved = CubieCube()
        val s = coord(solved)
        reps[s] = solved
        queue[tail++] = s
        while (head < tail) {
            val c = queue[head++]
            val rep = reps[c]!!
            for (mi in 0 until k) {
                val n = rep.multiply(CubieCube.MOVES[moves[mi]])
                val nc = coord(n)
                table[c * k + mi] = nc
                if (reps[nc] == null) {
                    reps[nc] = n
                    queue[tail++] = nc
                }
            }
        }
        check(tail == size) { "Move table incomplete: $tail / $size" }
        return table
    }

    private data class PruningTableConfig(
        val coordinates: PruningCoordinates,
        val moves: PruningMoves,
        val goal: PruningGoal
    )

    private data class PruningCoordinates(val firstCount: Int, val secondCount: Int)
    private data class PruningMoves(val first: IntArray, val second: IntArray, val count: Int)
    private data class PruningGoal(val first: Int, val second: Int)

    private fun buildPrune(config: PruningTableConfig): ByteArray {
        val (n1, n2) = config.coordinates
        val (move1, move2, nMoves) = config.moves
        val (goal1, goal2) = config.goal
        val size = n1 * n2
        val dist = ByteArray(size) { -1 }
        val queue = IntArray(size)
        var head = 0
        var tail = 0
        val start = goal1 * n2 + goal2
        dist[start] = 0
        queue[tail++] = start
        while (head < tail) {
            val idx = queue[head++]
            val a = idx / n2
            val b = idx % n2
            val d = (dist[idx] + 1).toByte()
            for (m in 0 until nMoves) {
                val ni = move1[a * nMoves + m] * n2 + move2[b * nMoves + m]
                if (dist[ni] < 0) {
                    dist[ni] = d
                    queue[tail++] = ni
                }
            }
        }
        return dist
    }
}