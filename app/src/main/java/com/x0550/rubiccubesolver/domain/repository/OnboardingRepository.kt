package com.x0550.rubiccubesolver.domain.repository

import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    fun hasSeenScanInstructions(): Flow<Boolean>

    suspend fun markScanInstructionsSeen()
}