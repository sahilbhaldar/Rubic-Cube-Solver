package com.x0550.rubiccubesolver.ui.theme

import androidx.annotation.DimenRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.x0550.rubiccubesolver.R

/**
 * Text styles used on the scan screen. Edit these to change size/weight/spacing of on-screen text.
 */
object AppTextStyles {

    /** "Face 1 of 6: WHITE side" heading in the top card. */
    val scanTitle: TextStyle
        @Composable get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = textDimension(R.dimen.dimen_22sp),
        lineHeight = textDimension(R.dimen.dimen_28sp)
    )

    /** Camera-facing side and orientation, e.g. "Keep GREEN at the BOTTOM." */
    val scanInstruction: TextStyle
        @Composable get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = textDimension(R.dimen.dimen_18sp),
        lineHeight = textDimension(R.dimen.dimen_24sp)
    )

    /** Text on the Capture / Recapture / Confirm / Start Over buttons. */
    val button: TextStyle
        @Composable get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = textDimension(R.dimen.dimen_18sp)
    )

    /** Title of the error card shown when the scanned cube is invalid. */
    val errorTitle: TextStyle
        @Composable get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = textDimension(R.dimen.dimen_20sp),
        lineHeight = textDimension(R.dimen.dimen_26sp)
    )

    /** Body text of the error card. */
    val errorBody: TextStyle
        @Composable get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = textDimension(R.dimen.dimen_16sp),
        lineHeight = textDimension(R.dimen.dimen_22sp)
    )

    /** "Step 3 of 20" on the solve screen. */
    val stepCounter: TextStyle
        @Composable get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = textDimension(R.dimen.dimen_15sp)
    )

    /** Main move instruction, e.g. "Turn RIGHT (red) clockwise 90°". */
    val moveTitle: TextStyle
        @Composable get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = textDimension(R.dimen.dimen_24sp),
        lineHeight = textDimension(R.dimen.dimen_30sp)
    )

    /** Plain-language hint under the move, e.g. "Front column moves UP ↑". */
    val moveHint: TextStyle
        @Composable get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = textDimension(R.dimen.dimen_18sp),
        lineHeight = textDimension(R.dimen.dimen_24sp)
    )

    /** Full move sequence strip, e.g. "R U R' U' ...". */
    val moveSequence: TextStyle
        @Composable get() = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = textDimension(R.dimen.dimen_16sp),
        lineHeight = textDimension(R.dimen.dimen_22sp)
    )
}

@Composable
internal fun textDimension(@DimenRes resourceId: Int): TextUnit {
    val dimension = dimensionResource(resourceId)
    return with(LocalDensity.current) { dimension.toSp() }
}