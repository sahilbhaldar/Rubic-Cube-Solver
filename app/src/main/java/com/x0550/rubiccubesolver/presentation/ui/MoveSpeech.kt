package com.x0550.rubiccubesolver.presentation.ui

import android.speech.tts.TextToSpeech
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.x0550.rubiccubesolver.R
import com.x0550.rubiccubesolver.domain.model.CubeMove
import com.x0550.rubiccubesolver.domain.model.Face
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
internal fun MoveSpeech(
    enabled: Boolean,
    move: CubeMove?,
    replayKey: Int,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    var initialized by remember { mutableStateOf(false) }
    var errorRes by remember { mutableStateOf<Int?>(null) }
    var speechErrorShown by remember { mutableStateOf(false) }
    var active by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(context) {
        var disposed = false
        val tts = TextToSpeech(context) { status ->
            scope.launch {
                if (!disposed) {
                    if (status == TextToSpeech.SUCCESS) initialized = true
                    else errorRes = R.string.speech_unavailable
                }
            }
        }
        engine = tts
        onDispose {
            disposed = true
            tts.stop()
            tts.shutdown()
        }
    }

    DisposableEffect(lifecycleOwner, engine) {
        val observer = LifecycleEventObserver { _, _ ->
            active = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (!active) engine?.stop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(enabled, initialized, move, replayKey, active) {
        val tts = engine ?: return@LaunchedEffect
        tts.stop()
        if (!enabled || !initialized || !active) return@LaunchedEffect
        // All bundled speech strings currently use the default English resources.
        val languageLocale = Locale.ENGLISH
        val language = tts.setLanguage(languageLocale)
        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            errorRes = R.string.speech_unavailable
            return@LaunchedEffect
        }
        val announcement = moveAnnouncement(move)
        val formatArgs = announcement.formatArgs.map { resourceId -> context.getString(resourceId) }
        val text = context.getString(announcement.messageRes, *formatArgs.toTypedArray())
        if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cube-move") == TextToSpeech.ERROR) {
            errorRes = R.string.speech_unavailable
        }
    }

    LaunchedEffect(enabled, errorRes) {
        if (enabled && errorRes != null && !speechErrorShown) {
            snackbarHostState.showSnackbar(context.getString(R.string.speech_unavailable))
            speechErrorShown = true
            errorRes = null
        }
    }
}

internal data class MoveAnnouncement(val messageRes: Int, val formatArgs: List<Int> = emptyList())

internal fun moveAnnouncement(move: CubeMove?): MoveAnnouncement {
    if (move == null) return MoveAnnouncement(R.string.speech_announcement_solved)
    val sideRes = when (move.face) {
        Face.UP -> R.string.speech_face_top
        Face.DOWN -> R.string.speech_face_bottom
        Face.RIGHT -> R.string.speech_face_right
        Face.LEFT -> R.string.speech_face_left
        Face.FRONT -> R.string.speech_face_front
        Face.BACK -> R.string.speech_face_back
    }
    val directionRes = when {
        move.double -> R.string.speech_direction_half
        move.clockwise -> R.string.speech_direction_clockwise
        else -> R.string.speech_direction_counter_clockwise
    }
    return MoveAnnouncement(R.string.speech_announcement_move, listOf(sideRes, directionRes))
}