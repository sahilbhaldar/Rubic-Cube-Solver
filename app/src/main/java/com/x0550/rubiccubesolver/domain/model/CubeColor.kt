package com.x0550.rubiccubesolver.domain.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.x0550.rubiccubesolver.R

enum class CubeColor(val symbol: Char, val displayColor: Color, @StringRes val nameRes: Int) {
    WHITE('U', Color(0xFFF9F9F9), R.string.color_white),
    RED('R', Color(0xFFF10606), R.string.color_red),
    GREEN('F', Color(0xFF388E3C), R.string.color_green),
    YELLOW('D', Color(0xFFFFBD1C), R.string.color_yellow),
    ORANGE('L', Color(0xFFFF6F00), R.string.color_orange),
    BLUE('B', Color(0xFF1976D2), R.string.color_blue),
    UNKNOWN('?', Color(0xFF424242), R.string.color_unknown);

    companion object {
        fun fromChar(char: Char): CubeColor = entries.find { it.symbol == char } ?: UNKNOWN
    }
}

enum class Face(
    val centerColor: CubeColor,
    @StringRes val titleRes: Int,
    @StringRes val instructionRes: Int
) {
    UP(CubeColor.WHITE, R.string.face_white_title, R.string.face_white_instruction),
    RIGHT(CubeColor.RED, R.string.face_red_title, R.string.face_red_instruction),
    FRONT(CubeColor.GREEN, R.string.face_green_title, R.string.face_green_instruction),
    DOWN(CubeColor.YELLOW, R.string.face_yellow_title, R.string.face_yellow_instruction),
    LEFT(CubeColor.ORANGE, R.string.face_orange_title, R.string.face_orange_instruction),
    BACK(CubeColor.BLUE, R.string.face_blue_title, R.string.face_blue_instruction)
}

data class CubeMove(
    val face: Face,
    val clockwise: Boolean = true,
    val double: Boolean = false,
    val notation: String = ""
)