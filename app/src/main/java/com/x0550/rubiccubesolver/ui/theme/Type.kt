package com.x0550.rubiccubesolver.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.x0550.rubiccubesolver.R

// Set of Material typography styles to start with
@Composable
fun appTypography() = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = textDimension(R.dimen.dimen_16sp),
        lineHeight = textDimension(R.dimen.dimen_24sp),
        letterSpacing = textDimension(R.dimen.dimen_0_5sp)
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = textDimension(R.dimen.dimen_22sp),
        lineHeight = textDimension(R.dimen.dimen_28sp),
        letterSpacing = textDimension(R.dimen.dimen_0sp)
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = textDimension(R.dimen.dimen_11sp),
        lineHeight = textDimension(R.dimen.dimen_16sp),
        letterSpacing = textDimension(R.dimen.dimen_0_5sp)
    )
    */
)