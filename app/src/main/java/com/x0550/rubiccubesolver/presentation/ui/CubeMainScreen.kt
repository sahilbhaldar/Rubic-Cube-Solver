package com.x0550.rubiccubesolver.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import com.x0550.rubiccubesolver.R
import com.x0550.rubiccubesolver.domain.model.CubeColor
import com.x0550.rubiccubesolver.domain.model.Face
import com.x0550.rubiccubesolver.presentation.viewmodel.CubeScanUiState
import com.x0550.rubiccubesolver.presentation.viewmodel.CubeSolutionUiState
import com.x0550.rubiccubesolver.presentation.viewmodel.CubeViewModel
import com.x0550.rubiccubesolver.ui.theme.AppTextStyles

@Composable
fun CubeMainScreen(
    viewModel: CubeViewModel,
    onCloseApp: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(dimensionResource(R.dimen.dimen_24dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.camera_permission_required),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.dimen_8dp)))
                Text(
                    text = stringResource(R.string.camera_permission_desc),
                    textAlign = TextAlign.Center,
                    color = Color.LightGray
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.dimen_16dp)))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(R.string.grant_permission_btn))
                }
            }
        }
        return
    }

    // Step 3 View: Display Solution & Animation
    val definition = uiState.scannedDefinition
    if (uiState.solutionReady && definition != null) {
        CubeSolverVisualizer(
            solution = CubeSolutionUiState(
                definition = definition,
                moves = uiState.moves,
                currentStepIndex = uiState.currentStepIndex
            ),
            actions = CubeSolverActions(
                next = viewModel::nextStep,
                previous = viewModel::previousStep,
                reset = viewModel::reset,
                closeApp = onCloseApp
            ),
            snackbarHostState = snackbarHostState
        )
        return
    }

    // Solving state
    if (uiState.isSolving) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(dimensionResource(R.dimen.dimen_16dp)))
                Text(stringResource(R.string.scan_solving), color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
        }
        return
    }

    // Step 2: Camera Capture Flow
    val pending = uiState.pendingFaceColors
    var editingFace by remember { mutableStateOf<Face?>(null) }
    var editedFaceColors by remember { mutableStateOf<List<CubeColor>>(emptyList()) }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraScanOverlay(
            analyzer = viewModel.getAnalyzer(),
            previewColors = pending ?: uiState.currentFrameColors,
            snackbarHostState = snackbarHostState,
            onStickerTap = if (pending != null) viewModel::cycleStickerColor else null
        )

        FaceScanControls(uiState, viewModel)

        if (uiState.currentFaceIndex >= viewModel.facesOrder.size) {
            Button(
                onClick = viewModel::retrySolve,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = dimensionResource(R.dimen.dimen_116dp))
            ) {
                Text(stringResource(R.string.scan_solve_corrected), style = AppTextStyles.button)
            }
        }

        CapturedSidesBar(
            uiState = uiState,
            facesOrder = viewModel.facesOrder,
            onEditFace = { face, colors ->
                editingFace = face
                editedFaceColors = colors
            }
        )

        uiState.errorMessage?.let { error -> ScanErrorCard(error, viewModel) }
    }

    editingFace?.let { face ->
        EditCapturedFaceDialog(
            face = face,
            colors = editedFaceColors,
            onUpdate = { colors -> viewModel.updateCapturedFace(face, colors) },
            onDismiss = { editingFace = null }
        )
    }

    if (uiState.showScanInstructionSheet) {
        ScanInstructionSheet(onDismiss = viewModel::dismissScanInstructions)
    }
}

@Composable
private fun BoxScope.FaceScanControls(uiState: CubeScanUiState, viewModel: CubeViewModel) {
    val currentFace = viewModel.facesOrder.getOrNull(uiState.currentFaceIndex) ?: return
    val pending = uiState.pendingFaceColors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.dimen_16dp),
                top = dimensionResource(R.dimen.dimen_16dp),
                end = dimensionResource(R.dimen.dimen_80dp),
                bottom = dimensionResource(R.dimen.dimen_16dp)
            )
            .align(Alignment.TopCenter),
        shape = RoundedCornerShape(dimensionResource(R.dimen.dimen_12dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.dimen_16dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(
                    R.string.scan_face_heading,
                    uiState.currentFaceIndex + 1,
                    viewModel.facesOrder.size,
                    stringResource(currentFace.titleRes)
                ),
                style = AppTextStyles.scanTitle,
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.dimen_6dp)))
            Text(
                text = if (pending != null) stringResource(R.string.scan_review_instruction)
                else stringResource(currentFace.instructionRes),
                style = AppTextStyles.scanInstruction,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFE082),
                textAlign = TextAlign.Center
            )
        }
    }

    if (pending != null) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = dimensionResource(R.dimen.dimen_16dp),
                    end = dimensionResource(R.dimen.dimen_16dp),
                    bottom = dimensionResource(R.dimen.dimen_116dp)
                )
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_12dp))
        ) {
            OutlinedButton(
                onClick = viewModel::recaptureFace,
                modifier = Modifier.weight(1f).height(dimensionResource(R.dimen.dimen_56dp)),
                shape = RoundedCornerShape(dimensionResource(R.dimen.dimen_28dp)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                )
            ) {
                Text(stringResource(R.string.scan_recapture), style = AppTextStyles.button)
            }
            Button(
                onClick = viewModel::confirmFace,
                modifier = Modifier.weight(1f).height(dimensionResource(R.dimen.dimen_56dp)),
                shape = RoundedCornerShape(dimensionResource(R.dimen.dimen_28dp)),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.scan_confirm), style = AppTextStyles.button)
            }
        }
    } else {
        Button(
            onClick = viewModel::captureCurrentFace,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = dimensionResource(R.dimen.dimen_116dp))
                .height(dimensionResource(R.dimen.dimen_56dp))
                .fillMaxWidth(0.7f),
            shape = RoundedCornerShape(dimensionResource(R.dimen.dimen_28dp)),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                stringResource(R.string.scan_capture_color, stringResource(currentFace.centerColor.nameRes)),
                style = AppTextStyles.button
            )
        }
    }
}

@Composable
private fun BoxScope.CapturedSidesBar(
    uiState: CubeScanUiState,
    facesOrder: List<Face>,
    onEditFace: (Face, List<CubeColor>) -> Unit
) {
    Surface(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.85f)
    ) {
        Column(
            Modifier.padding(
                horizontal = dimensionResource(R.dimen.dimen_12dp),
                vertical = dimensionResource(R.dimen.dimen_8dp)
            )
        ) {
            Text(
                stringResource(R.string.scan_captured_sides),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.dimen_6dp)))
            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_6dp))) {
                facesOrder.forEachIndexed { index, face ->
                    CapturedSide(
                        face = face,
                        index = index,
                        currentFaceIndex = uiState.currentFaceIndex,
                        colors = uiState.capturedFaces[face],
                        onEditFace = onEditFace
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.CapturedSide(
    face: Face,
    index: Int,
    currentFaceIndex: Int,
    colors: List<CubeColor>?,
    onEditFace: (Face, List<CubeColor>) -> Unit
) {
    val colorName = stringResource(face.centerColor.nameRes)
    val sideDescription = stringResource(
        if (colors != null) R.string.scan_side_captured_description
        else R.string.scan_side_pending_description,
        colorName
    )
    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = { colors?.let { onEditFace(face, it.toList()) } },
            enabled = colors != null,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).semantics {
                contentDescription = sideDescription
            },
            shape = RoundedCornerShape(dimensionResource(R.dimen.dimen_4dp)),
            color = Color(0xFF252525),
            border = BorderStroke(
                dimensionResource(R.dimen.dimen_2dp),
                if (index == currentFaceIndex) Color.Yellow else Color.Gray
            )
        ) {
            if (colors != null) {
                StickerGrid(colors, Modifier.padding(dimensionResource(R.dimen.dimen_4dp)))
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.scan_slot_number, index + 1), color = Color.Gray)
                }
            }
        }
        Text(colorName, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun BoxScope.ScanErrorCard(error: String, viewModel: CubeViewModel) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.dimen_24dp)),
            shape = RoundedCornerShape(dimensionResource(R.dimen.dimen_16dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier.padding(dimensionResource(R.dimen.dimen_20dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.scan_error_title),
                    style = AppTextStyles.errorTitle,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.dimen_8dp)))
                Text(
                    error,
                    style = AppTextStyles.errorBody,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.dimen_16dp)))
                OutlinedButton(onClick = viewModel::reviewCapturedFaces, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.scan_edit_sides), style = AppTextStyles.button)
                }
                Spacer(Modifier.height(dimensionResource(R.dimen.dimen_8dp)))
                Button(
                    onClick = viewModel::reset,
                    modifier = Modifier.fillMaxWidth().height(dimensionResource(R.dimen.dimen_56dp)),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.dimen_28dp))
                ) {
                    Text(stringResource(R.string.scan_start_over), style = AppTextStyles.button)
                }
            }
        }
    }
}

@Composable
private fun EditCapturedFaceDialog(
    face: Face,
    colors: List<CubeColor>,
    onUpdate: (List<CubeColor>) -> Unit,
    onDismiss: () -> Unit
) {
    var editedColors by remember(face, colors) { mutableStateOf(colors) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.scan_edit_side_title, stringResource(face.centerColor.nameRes))) },
        text = {
            Column {
                Text(stringResource(R.string.scan_edit_side_instruction))
                Spacer(Modifier.height(dimensionResource(R.dimen.dimen_16dp)))
                StickerGrid(
                    colors = editedColors,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    onStickerTap = { index ->
                        val choices = CubeColor.entries.filter { it != CubeColor.UNKNOWN }
                        editedColors = editedColors.toMutableList().apply {
                            this[index] = choices[(choices.indexOf(this[index]) + 1) % choices.size]
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onUpdate(editedColors); onDismiss() }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun StickerGrid(
    colors: List<CubeColor>,
    modifier: Modifier = Modifier,
    onStickerTap: ((Int) -> Unit)? = null
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_3dp))) {
        repeat(3) { row ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_3dp))) {
                repeat(3) { column ->
                    val index = row * 3 + column
                    val stickerDescription = stringResource(
                        if (index == 4) R.string.scan_center_description else R.string.scan_sticker_description,
                        row + 1,
                        column + 1,
                        stringResource(colors[index].nameRes)
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(colors[index].displayColor, RoundedCornerShape(dimensionResource(R.dimen.dimen_2dp)))
                            .semantics {
                                contentDescription = stickerDescription
                            }
                            .then(
                                if (onStickerTap != null && index != 4) {
                                    Modifier.clickable { onStickerTap(index) }
                                } else Modifier
                            )
                    )
                }
            }
        }
    }
}