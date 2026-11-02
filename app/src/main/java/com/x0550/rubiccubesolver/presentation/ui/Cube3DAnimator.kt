package com.x0550.rubiccubesolver.presentation.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.x0550.rubiccubesolver.R
import com.x0550.rubiccubesolver.domain.model.CubeColor
import com.x0550.rubiccubesolver.domain.model.CubeMove
import com.x0550.rubiccubesolver.domain.model.Face
import com.x0550.rubiccubesolver.presentation.ui.cube3d.CubeModel3D
import com.x0550.rubiccubesolver.presentation.ui.cube3d.IVec
import com.x0550.rubiccubesolver.presentation.ui.cube3d.normal
import com.x0550.rubiccubesolver.presentation.viewmodel.CubeSolutionUiState
import com.x0550.rubiccubesolver.ui.theme.AppTextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val DEFAULT_YAW = -35f
private const val DEFAULT_PITCH = 28f

@Composable
fun CubeSolverVisualizer(
    solution: CubeSolutionUiState,
    actions: CubeSolverActions,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val startDefinition = solution.definition
    val moves = solution.moves
    val currentIndex = solution.currentStepIndex
    // states[i] = cube before move i; states[moves.size] = solved cube.
    val states = remember(startDefinition, moves) {
        moves.runningFold(CubeModel3D.fromFacelets(startDefinition)) { cube, mv -> cube.turn(mv.face, mv.quarterTurns) }
    }
    val done = currentIndex >= moves.size
    val currentMove = moves.getOrNull(currentIndex)
    var showExitConfirmation by rememberSaveable { mutableStateOf(false) }

    BackHandler { showExitConfirmation = true }
    if (showExitConfirmation) {
        CubicExitConfirmationDialog(
            solved = done,
            onDismiss = { showExitConfirmation = false },
            onConfirm = {
                showExitConfirmation = false
                actions.closeApp()
            }
        )
    }

    val progress = remember { Animatable(0f) }
    val arrowStrokeWidth = dimensionResource(R.dimen.dimen_7dp)
    val arrowOutlineWidth = dimensionResource(R.dimen.dimen_4dp)
    val arrowHeadLength = dimensionResource(R.dimen.dimen_22dp)
    val arrowHeadWidth = dimensionResource(R.dimen.dimen_14dp)
    val arrowDimensions = ArrowDimensions(
        arrowStrokeWidth,
        arrowOutlineWidth,
        arrowHeadLength,
        arrowHeadWidth
    )
    val yaw = remember { Animatable(DEFAULT_YAW) }
    val pitch = remember { Animatable(DEFAULT_PITCH) }
    var dragYaw by remember { mutableFloatStateOf(0f) }
    var dragPitch by remember { mutableFloatStateOf(0f) }
    var replayKey by remember { mutableIntStateOf(0) }
    var speechEnabled by rememberSaveable { mutableStateOf(false) }
    MoveSpeech(speechEnabled, currentMove, replayKey, snackbarHostState)

    LaunchedEffect(currentIndex, replayKey) {
        progress.snapTo(0f)
        dragYaw = 0f
        dragPitch = 0f
        val (targetYaw, targetPitch) = currentMove?.face?.let(::cameraFor) ?: (DEFAULT_YAW to DEFAULT_PITCH)
        launch { yaw.animateTo(targetYaw, tween(500)) }
        launch { pitch.animateTo(targetPitch, tween(500)) }
        if (currentMove != null) {
            delay(700)
            progress.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(dimensionResource(R.dimen.dimen_16dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SolutionHeader(
            state = SolutionHeaderState(done, currentIndex, moves.size, speechEnabled),
            actions = actions,
            onSpeechEnabledChange = { speechEnabled = it }
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.dimen_4dp)))
        MoveInstructions(currentMove, moves.isEmpty())

        // 3D cube
        val turning = currentMove != null && progress.value < 1f
        val model = if (turning || currentMove == null) states[currentIndex] else states[currentIndex + 1]
        val turnDegrees = if (turning) progress.value * currentMove!!.signedDegrees else 0f

        CubeScene(
            state = CubeRenderState(
                model = model,
                camera = CubeCameraState(
                    yaw = yaw.value + dragYaw,
                    pitch = (pitch.value + dragPitch).coerceIn(-85f, 85f)
                ),
                turn = currentMove?.let { move ->
                    CubeTurnState(
                        move = move,
                        turnFace = if (turning) move.face else null,
                        degrees = turnDegrees,
                        arrowDimensions = arrowDimensions
                    )
                }
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        dragYaw += drag.x * 0.4f
                        dragPitch = (dragPitch + drag.y * 0.4f).coerceIn(-80f, 80f)
                    }
                }
                .semantics { contentDescription = context.getString(R.string.solve_drag_hint) }
        )

        SolutionControls(
            done = done,
            currentIndex = currentIndex,
            actions = actions,
            onRequestExit = { showExitConfirmation = true },
            onReplay = { replayKey++ }
        )
    }
}

private data class SolutionHeaderState(
    val done: Boolean,
    val currentIndex: Int,
    val moveCount: Int,
    val speechEnabled: Boolean
)

@Composable
private fun SolutionHeader(
    state: SolutionHeaderState,
    actions: CubeSolverActions,
    onSpeechEnabledChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (state.done) stringResource(R.string.solve_done)
            else stringResource(R.string.solve_step_counter, state.currentIndex + 1, state.moveCount),
            style = AppTextStyles.stepCounter,
            color = Color.LightGray
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconToggleButton(
                checked = state.speechEnabled,
                onCheckedChange = onSpeechEnabledChange
            ) {
                Icon(
                    imageVector = if (state.speechEnabled) Icons.AutoMirrored.Filled.VolumeUp
                    else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = stringResource(
                        if (state.speechEnabled) R.string.solve_mute_speech else R.string.solve_enable_speech
                    ),
                    tint = if (state.speechEnabled) Color(0xFFFFE082) else Color.LightGray
                )
            }
            TextButton(onClick = actions.reset) {
                Text(stringResource(R.string.solve_scan_again), color = Color.LightGray)
            }
        }
    }
}

@Composable
private fun MoveInstructions(move: CubeMove?, alreadySolved: Boolean) {
    val context = LocalContext.current
    val title = move?.let { describeMove(it, context) } ?: if (alreadySolved) {
        context.getString(R.string.solve_already_solved)
    } else {
        context.getString(R.string.solve_cube_solved)
    }
    Text(title, style = AppTextStyles.moveTitle, color = Color.White, textAlign = TextAlign.Center)
    if (move != null) {
        Spacer(Modifier.height(dimensionResource(R.dimen.dimen_6dp)))
        Text(
            stringResource(
                R.string.solve_keep_orientation,
                stringResource(CubeColor.WHITE.nameRes),
                stringResource(CubeColor.GREEN.nameRes)
            ),
            style = AppTextStyles.stepCounter,
            color = Color.LightGray,
            textAlign = TextAlign.Center
        )
    } else {
        Spacer(Modifier.height(dimensionResource(R.dimen.dimen_6dp)))
        Text(
            finishHint(context),
            style = AppTextStyles.moveHint,
            color = Color(0xFFFFE082),
            textAlign = TextAlign.Center
        )
    }
}

private fun finishHint(context: Context): String = context.getString(
    R.string.solve_finish_hint,
    context.getString(CubeColor.WHITE.nameRes),
    context.getString(CubeColor.GREEN.nameRes)
)

private data class CubeCameraState(val yaw: Float, val pitch: Float)

private data class CubeTurnState(
    val move: CubeMove,
    val turnFace: Face?,
    val degrees: Float,
    val arrowDimensions: ArrowDimensions
)

private data class CubeRenderState(
    val model: CubeModel3D,
    val camera: CubeCameraState,
    val turn: CubeTurnState?
)

@Composable
private fun CubeScene(state: CubeRenderState, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val view = ViewTransform(state.camera.yaw, state.camera.pitch)
            drawCube(state.model, view, state.turn?.turnFace, state.turn?.degrees ?: 0f)
            state.turn?.let { drawTurnArrow(it.move, view, it.arrowDimensions) }
        }
        Text(
            stringResource(R.string.solve_drag_hint),
            style = AppTextStyles.stepCounter,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SolutionControls(
    done: Boolean,
    currentIndex: Int,
    actions: CubeSolverActions,
    onRequestExit: () -> Unit,
    onReplay: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_12dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (done) {
            OutlinedButton(
                onClick = actions.reset,
                modifier = Modifier.weight(1f).heightIn(min = dimensionResource(R.dimen.dimen_56dp)),
                contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.dimen_12dp))
            ) {
                Text(stringResource(R.string.solve_scan_cube), style = AppTextStyles.button)
            }
            Button(
                onClick = onRequestExit,
                modifier = Modifier.weight(1f).heightIn(min = dimensionResource(R.dimen.dimen_56dp)),
                contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.dimen_12dp))
            ) {
                Text(stringResource(R.string.solve_close_app), style = AppTextStyles.button)
            }
        } else {
            FilledTonalIconButton(
                onClick = actions.previous,
                enabled = currentIndex > 0,
                modifier = Modifier.size(dimensionResource(R.dimen.dimen_56dp))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.solve_previous_move)
                )
            }
            OutlinedButton(
                onClick = onReplay,
                modifier = Modifier.weight(1f).height(dimensionResource(R.dimen.dimen_56dp))
            ) {
                Text(stringResource(R.string.solve_replay), style = AppTextStyles.button)
            }
            Button(
                onClick = actions.next,
                modifier = Modifier.weight(1f).height(dimensionResource(R.dimen.dimen_56dp)),
                contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.dimen_12dp))
            ) {
                Text(stringResource(R.string.solve_next), style = AppTextStyles.button)
                Spacer(Modifier.width(dimensionResource(R.dimen.dimen_8dp)))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

private val CubeMove.quarterTurns: Int get() = if (double) 2 else if (clockwise) 1 else 3

/** Degrees of clockwise rotation (as seen looking at the face); negative = counter-clockwise. */
private val CubeMove.signedDegrees: Float get() = if (double) 180f else if (clockwise) 90f else -90f

/** Camera angles (yaw, pitch) that make the turning face visible. */
private fun cameraFor(face: Face): Pair<Float, Float> = when (face) {
    Face.UP, Face.RIGHT, Face.FRONT -> DEFAULT_YAW to DEFAULT_PITCH
    Face.LEFT -> 35f to DEFAULT_PITCH
    Face.DOWN -> DEFAULT_YAW to -DEFAULT_PITCH
    Face.BACK -> -145f to DEFAULT_PITCH
}

/** Title + plain-language hint, assuming WHITE on top and GREEN facing the user. */
private fun describeMove(move: CubeMove, context: Context): String =
    context.getString(
        R.string.solve_turn_title,
        context.getString(faceNameResource(move.face), context.getString(faceColor(move.face).nameRes)),
        context.getString(turnAmountResource(move))
    )

private fun turnAmountResource(move: CubeMove): Int = when {
    move.double -> R.string.solve_turn_half
    move.clockwise -> R.string.solve_turn_clockwise
    else -> R.string.solve_turn_counter_clockwise
}

private fun faceNameResource(face: Face): Int = when (face) {
        Face.UP -> R.string.solve_face_top
        Face.RIGHT -> R.string.solve_face_right
        Face.FRONT -> R.string.solve_face_front
        Face.DOWN -> R.string.solve_face_bottom
        Face.LEFT -> R.string.solve_face_left
        Face.BACK -> R.string.solve_face_back
}

private fun faceColor(face: Face): CubeColor = when (face) {
        Face.UP -> CubeColor.WHITE
        Face.RIGHT -> CubeColor.RED
        Face.FRONT -> CubeColor.GREEN
        Face.DOWN -> CubeColor.YELLOW
        Face.LEFT -> CubeColor.ORANGE
        Face.BACK -> CubeColor.BLUE
}

// ---------------------------------------------------------------------------------------------------------------
// Lightweight 3D rendering on Canvas
// ---------------------------------------------------------------------------------------------------------------

private data class V3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: V3) = V3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: V3) = V3(x - o.x, y - o.y, z - o.z)
    operator fun times(k: Float) = V3(x * k, y * k, z * k)
    infix fun dot(o: V3) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: V3) = V3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
}

private fun IVec.toV3() = V3(x.toFloat(), y.toFloat(), z.toFloat())

/** Rodrigues rotation of [v] about unit [axis] by [rad] (counter-clockwise seen from the axis tip). */
private fun rotate(v: V3, axis: V3, rad: Float): V3 {
    val c = cos(rad)
    val s = sin(rad)
    return v * c + (axis cross v) * s + axis * ((axis dot v) * (1 - c))
}

private class ViewTransform(yawDeg: Float, pitchDeg: Float) {
    private val cy = cos(yawDeg * PI.toFloat() / 180f)
    private val sy = sin(yawDeg * PI.toFloat() / 180f)
    private val cp = cos(pitchDeg * PI.toFloat() / 180f)
    private val sp = sin(pitchDeg * PI.toFloat() / 180f)

    /** World -> view space. Camera sits at (0, 0, CAMERA_DIST) looking towards -z. */
    fun apply(p: V3): V3 {
        val x1 = p.x * cy + p.z * sy
        val z1 = -p.x * sy + p.z * cy
        val y2 = p.y * cp - z1 * sp
        val z2 = p.y * sp + z1 * cp
        return V3(x1, y2, z2)
    }

    private data class ArrowDimensions(
        val strokeWidth: Dp,
        val outlineWidth: Dp,
        val headLength: Dp,
        val headWidth: Dp
    )

    companion object {
        const val CAMERA_DIST = 11f
        val CAMERA = V3(0f, 0f, CAMERA_DIST)
    }
}

private fun DrawScope.project(v: V3): Offset {
    val scale = min(size.width, size.height) / 6.2f
    val persp = ViewTransform.CAMERA_DIST / (ViewTransform.CAMERA_DIST - v.z)
    return Offset(size.width / 2f + v.x * scale * persp, size.height / 2f - v.y * scale * persp)
}

private val AXES = listOf(
    IVec(1, 0, 0), IVec(-1, 0, 0), IVec(0, 1, 0), IVec(0, -1, 0), IVec(0, 0, 1), IVec(0, 0, -1)
)

private fun perpendicularAxes(n: IVec): Pair<V3, V3> {
    val u = if (n.x != 0) V3(0f, 1f, 0f) else V3(1f, 0f, 0f)
    val nv = n.toV3()
    return u to (nv cross u)
}

private fun DrawScope.drawCube(model: CubeModel3D, view: ViewTransform, turnFace: Face?, turnDegrees: Float) {
    val n = turnFace?.normal
    val nV = n?.toV3()
    val rad = -turnDegrees * PI.toFloat() / 180f

    fun toView(p: V3, inLayer: Boolean): V3 = view.apply(if (inLayer && nV != null) rotate(p, nV, rad) else p)

    val (layer, rest) = model.cubies.partition { n != null && (it.pos dot n) == 1 }

    // The turning layer and the rest are separated by a plane; draw the group farther from the camera first.
    val groups = if (nV == null) listOf(rest) else {
        val planeNormal = view.apply(nV)
        val planePoint = view.apply(nV * 0.5f)
        val cameraOnLayerSide = (planeNormal dot (ViewTransform.CAMERA - planePoint)) > 0f
        if (cameraOnLayerSide) listOf(rest, layer) else listOf(layer, rest)
    }

    for (group in groups) {
        val inLayer = nV != null && group === layer
        val sorted = group.sortedByDescending { c ->
            val v = toView(c.pos.toV3(), inLayer) - ViewTransform.CAMERA
            v dot v
        }
        for (cubie in sorted) {
            val center = cubie.pos.toV3()
            for (axis in AXES) {
                val a = axis.toV3()
                val faceCenterView = toView(center + a * 0.5f, inLayer)
                val normalView = toView(center + a, inLayer) - toView(center, inLayer)
                if ((normalView dot (ViewTransform.CAMERA - faceCenterView)) <= 0f) continue

                val (u, v) = perpendicularAxes(axis)
                val transform: (V3) -> V3 = { point -> toView(point, inLayer) }
                drawQuad(center + a * 0.5f, u to v, 0.5f, Color(0xFF111111), transform)
                cubie.stickers[axis]?.let { color ->
                    drawQuad(center + a * 0.51f, u to v, 0.42f, color.displayColor, transform)
                }
            }
        }
    }
}

private fun DrawScope.drawQuad(
    center: V3,
    axes: Pair<V3, V3>,
    half: Float,
    color: Color,
    transform: (V3) -> V3
) {
    val corners = listOf(
        center + axes.first * half + axes.second * half,
        center - axes.first * half + axes.second * half,
        center - axes.first * half - axes.second * half,
        center + axes.first * half - axes.second * half
    ).map { project(transform(it)) }
    val path = Path().apply {
        moveTo(corners[0].x, corners[0].y)
        for (i in 1 until 4) lineTo(corners[i].x, corners[i].y)
        close()
    }
    drawPath(path, color)
}

private data class ArrowDimensions(
    val strokeWidth: Dp,
    val outlineWidth: Dp,
    val headLength: Dp,
    val headWidth: Dp
)

/** Curved arrow on the turning face showing the direction (as seen looking straight at that face). */
private fun DrawScope.drawTurnArrow(
    move: CubeMove,
    view: ViewTransform,
    dimensions: ArrowDimensions
) {
    val n = move.face.normal
    val nV = n.toV3()
    val faceCenter = nV * 1.56f
    if ((view.apply(nV) dot (ViewTransform.CAMERA - view.apply(faceCenter))) <= 0f) return

    val u = perpendicularAxes(n).first
    val w = nV cross u // u x w = n, so increasing angle = counter-clockwise seen from outside
    val sweep = (if (move.double) 300f else 240f) * PI.toFloat() / 180f
    val direction = if (move.clockwise || move.double) -1f else 1f
    val start = 110f * PI.toFloat() / 180f
    val radius = 1.0f
    val steps = 48
    val pts = (0..steps).map { i ->
        val t = start + direction * sweep * i / steps
        project(view.apply(faceCenter + u * (radius * cos(t)) + w * (radius * sin(t))))
    }

    val path = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
    }
    val strokePx = dimensions.strokeWidth.toPx()
    drawPath(
        path,
        Color.Black.copy(alpha = 0.7f),
        style = Stroke(strokePx + dimensions.outlineWidth.toPx(), cap = StrokeCap.Round)
    )
    drawPath(path, Color(0xFFFFEB3B), style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // Arrow head
    val tip = pts.last()
    val prev = pts[pts.size - 4]
    val dx = tip.x - prev.x
    val dy = tip.y - prev.y
    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
    val ux = dx / len
    val uy = dy / len
    val headLen = dimensions.headLength.toPx()
    val headWidth = dimensions.headWidth.toPx()
    val tipFar = Offset(tip.x + ux * headLen * 0.6f, tip.y + uy * headLen * 0.6f)
    val base = Offset(tipFar.x - ux * headLen, tipFar.y - uy * headLen)
    val head = Path().apply {
        moveTo(tipFar.x, tipFar.y)
        lineTo(base.x - uy * headWidth, base.y + ux * headWidth)
        lineTo(base.x + uy * headWidth, base.y - ux * headWidth)
        close()
    }
    drawPath(head, Color.Black.copy(alpha = 0.7f), style = Stroke(dimensions.outlineWidth.toPx(), join = StrokeJoin.Round))
    drawPath(head, Color(0xFFFFEB3B))
}