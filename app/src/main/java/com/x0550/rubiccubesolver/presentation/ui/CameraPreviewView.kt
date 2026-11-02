package com.x0550.rubiccubesolver.presentation.ui

import android.hardware.camera2.CaptureRequest
import android.view.MotionEvent
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.x0550.rubiccubesolver.R
import com.x0550.rubiccubesolver.data.analyzer.CubeFrameAnalyzer
import com.x0550.rubiccubesolver.domain.model.CubeColor
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraScanOverlay(
    analyzer: CubeFrameAnalyzer,
    previewColors: List<CubeColor>,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    /** When non-null the overlay is in review mode: dots are enlarged and tapping one reports its index (0..8). */
    onStickerTap: ((Int) -> Unit)? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val guideSize = dimensionResource(R.dimen.dimen_280dp)
    val guideStrokeWidth = dimensionResource(R.dimen.dimen_4dp)
    val stickerStrokeWidth = dimensionResource(R.dimen.dimen_2dp)
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var torchBusy by remember { mutableStateOf(false) }
    val currentStickerTap by rememberUpdatedState(onStickerTap)
    val context = LocalContext.current
    val previewView = remember(context) {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    val focusAt: (Float, Float) -> Unit = { x, y ->
        camera?.let { cam ->
            val point = previewView.meteringPointFactory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
            ).setAutoCancelDuration(5, TimeUnit.SECONDS).build()
            val result = cam.cameraControl.startFocusAndMetering(action)
            result.addListener({
                try {
                    result.get()
                } catch (_: java.util.concurrent.ExecutionException) {
                    Unit
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    CameraPreviewBinding(analyzer, previewView, snackbarHostState) { camera = it }

    DisposableEffect(camera, lifecycleOwner) {
        val cam = camera
        val observer = Observer<Int> { torchOn = it == TorchState.ON }
        cam?.cameraInfo?.torchState?.observe(lifecycleOwner, observer)
        onDispose {
            cam?.cameraInfo?.torchState?.removeObserver(observer)
        }
    }

    ConfigureCamera(camera)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                val guidePx = with(density) { guideSize.toPx() }
                analyzer.setGuideRegion(
                    viewWidth = size.width.toFloat(),
                    viewHeight = size.height.toFloat(),
                    left = (size.width - guidePx) / 2f,
                    top = (size.height - guidePx) / 2f,
                    size = guidePx
                )
            },
        contentAlignment = Alignment.Center
    ) {
        CameraPreviewSurface(
            previewView = previewView,
            stickerTapEnabled = currentStickerTap != null,
            onFocus = focusAt
        )

        val guidePx = with(density) { guideSize.toPx() }
        StickerGuideCanvas(
            state = StickerGuideState(
                guideSize,
                onStickerTap != null,
                previewColors,
                guideStrokeWidth,
                stickerStrokeWidth
            ),
            actions = StickerGuideActions(
                onStickerTap,
                onFocus = { x, y ->
                    focusAt((previewView.width - guidePx) / 2f + x, (previewView.height - guidePx) / 2f + y)
                }
            )
        )

        FlashlightControl(
            camera = camera,
            state = FlashlightState(torchOn, torchBusy),
            actions = FlashlightActions(
                setBusy = { torchBusy = it },
                setTorchOn = { torchOn = it }
            ),
            snackbarHostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun CameraPreviewBinding(
    analyzer: CubeFrameAnalyzer,
    previewView: PreviewView,
    snackbarHostState: SnackbarHostState,
    onCameraChanged: (Camera?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    DisposableEffect(context, lifecycleOwner, analyzer, previewView) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var disposed = false
        var provider: ProcessCameraProvider? = null
        var boundCamera: Camera? = null
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .build()
        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
            .also { it.surfaceProvider = previewView.surfaceProvider }
        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(analysisExecutor, analyzer) }

        providerFuture.addListener({
            if (!disposed) {
                try {
                    val cameraProvider = providerFuture.get()
                    provider = cameraProvider
                    boundCamera = cameraProvider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
                    )
                    onCameraChanged(boundCamera)
                    boundCamera?.cameraControl?.enableTorch(false)
                } catch (_: Exception) {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.camera_start_error))
                    }
                }
            }
        }, mainExecutor)

        onDispose {
            disposed = true
            boundCamera?.cameraControl?.enableTorch(false)
            imageAnalysis.clearAnalyzer()
            provider?.unbind(preview, imageAnalysis)
            preview.setSurfaceProvider(null)
            onCameraChanged(null)
            analysisExecutor.shutdown()
        }
    }
}

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
@Composable
private fun ConfigureCamera(camera: Camera?) {
    LaunchedEffect(camera) {
        val cam = camera ?: return@LaunchedEffect
        Camera2CameraControl.from(cam.cameraControl).setCaptureRequestOptions(
            CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, false)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, false)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
                .build()
        )
    }
}

@Composable
private fun CameraPreviewSurface(
    previewView: PreviewView,
    stickerTapEnabled: Boolean,
    onFocus: (Float, Float) -> Unit
) {
    AndroidView(
        factory = { previewView },
        update = { view ->
            view.setOnTouchListener { touchedView, event ->
                if (stickerTapEnabled) false
                else {
                    if (event.action == MotionEvent.ACTION_UP) {
                        touchedView.performClick()
                        onFocus(event.x, event.y)
                    }
                    true
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private data class StickerGuideState(
    val guideSize: Dp,
    val reviewing: Boolean,
    val previewColors: List<CubeColor>,
    val guideStrokeWidth: Dp,
    val stickerStrokeWidth: Dp
)

private data class StickerGuideActions(
    val onStickerTap: ((Int) -> Unit)?,
    val onFocus: (Float, Float) -> Unit
)

@Composable
private fun StickerGuideCanvas(state: StickerGuideState, actions: StickerGuideActions) {
    Canvas(
        modifier = Modifier
            .size(state.guideSize)
            .then(
                if (actions.onStickerTap != null) {
                    Modifier.pointerInput(actions.onStickerTap) {
                        detectTapGestures { pos ->
                            val col = (pos.x / (size.width / 3f)).toInt().coerceIn(0, 2)
                            val row = (pos.y / (size.height / 3f)).toInt().coerceIn(0, 2)
                            actions.onStickerTap.invoke(row * 3 + col)
                        }
                    }
                } else Modifier.pointerInput(actions.onFocus) {
                    detectTapGestures { pos -> actions.onFocus(pos.x, pos.y) }
                }
            )
    ) {
        val strokeWidth = state.guideStrokeWidth.toPx()
        val step = size.width / 3f
        if (state.reviewing) drawRect(color = Color.Black.copy(alpha = 0.55f), size = size)
        drawRect(color = Color.White, size = size, style = Stroke(width = strokeWidth))
        drawStickerGrid(step, strokeWidth)
        if (state.previewColors.size == 9) {
            drawStickerMarkers(state.previewColors, state.reviewing, step, state.stickerStrokeWidth.toPx())
        }
    }
}

private fun DrawScope.drawStickerGrid(step: Float, strokeWidth: Float) {
    for (index in 1..2) {
        drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = Offset(step * index, 0f),
            end = Offset(step * index, size.height),
            strokeWidth = strokeWidth / 2
        )
        drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = Offset(0f, step * index),
            end = Offset(size.width, step * index),
            strokeWidth = strokeWidth / 2
        )
    }
}

private fun DrawScope.drawStickerMarkers(
    colors: List<CubeColor>,
    reviewing: Boolean,
    step: Float,
    strokeWidth: Float
) {
    for (row in 0 until 3) {
        for (column in 0 until 3) {
            val radius = if (reviewing) step / 3f else step / 5f
            val center = Offset(column * step + step / 2f, row * step + step / 2f)
            val color = colors[row * 3 + column].displayColor
            drawCircle(color = color, radius = radius, center = center)
            drawCircle(color = Color.Black, radius = radius, center = center, style = Stroke(width = strokeWidth))
        }
    }
}

private data class FlashlightState(val isOn: Boolean, val isBusy: Boolean)

private data class FlashlightActions(
    val setBusy: (Boolean) -> Unit,
    val setTorchOn: (Boolean) -> Unit
)

@Composable
private fun FlashlightControl(
    camera: Camera?,
    state: FlashlightState,
    actions: FlashlightActions,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    IconToggleButton(
        checked = state.isOn,
        enabled = camera?.cameraInfo?.hasFlashUnit() == true && !state.isBusy,
        onCheckedChange = { enabled ->
            camera?.let { cam ->
                actions.setBusy(true)
                val result = cam.cameraControl.enableTorch(enabled)
                result.addListener({
                    try {
                        result.get()
                        actions.setTorchOn(enabled)
                    } catch (_: java.util.concurrent.ExecutionException) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.camera_torch_error)) }
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.camera_torch_error)) }
                    } finally {
                        actions.setBusy(false)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        },
        modifier = modifier
            .padding(dimensionResource(R.dimen.dimen_16dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(dimensionResource(R.dimen.dimen_12dp)))
    ) {
        Icon(
            imageVector = if (state.isOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
            contentDescription = stringResource(
                if (camera?.cameraInfo?.hasFlashUnit() != true) R.string.camera_torch_unavailable
                else if (state.isOn) R.string.camera_torch_off else R.string.camera_torch_on
            ),
            tint = if (state.isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}