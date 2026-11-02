package com.x0550.rubiccubesolver

import com.x0550.rubiccubesolver.domain.model.CubeMove
import com.x0550.rubiccubesolver.domain.model.Face
import com.x0550.rubiccubesolver.presentation.ui.MoveAnnouncement
import com.x0550.rubiccubesolver.presentation.ui.moveAnnouncement
import org.junit.Assert.assertEquals
import org.junit.Test

class MoveSpeechTest {
    @Test
    fun announcesEveryFaceAndDirection() {
        val labels = mapOf(
            Face.UP to R.string.speech_face_top, Face.DOWN to R.string.speech_face_bottom,
            Face.RIGHT to R.string.speech_face_right, Face.LEFT to R.string.speech_face_left,
            Face.FRONT to R.string.speech_face_front, Face.BACK to R.string.speech_face_back
        )
        for ((face, labelRes) in labels) {
            assertEquals(
                MoveAnnouncement(R.string.speech_announcement_move, listOf(labelRes, R.string.speech_direction_clockwise)),
                moveAnnouncement(CubeMove(face, clockwise = true, notation = ""))
            )
            assertEquals(
                MoveAnnouncement(R.string.speech_announcement_move, listOf(labelRes, R.string.speech_direction_counter_clockwise)),
                moveAnnouncement(CubeMove(face, clockwise = false, notation = ""))
            )
            assertEquals(
                MoveAnnouncement(R.string.speech_announcement_move, listOf(labelRes, R.string.speech_direction_half)),
                moveAnnouncement(CubeMove(face, double = true, notation = ""))
            )
        }
        assertEquals(
            MoveAnnouncement(R.string.speech_announcement_solved, emptyList()),
            moveAnnouncement(null)
        )
    }
}
