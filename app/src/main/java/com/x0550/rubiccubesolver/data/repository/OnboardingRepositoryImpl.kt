package com.x0550.rubiccubesolver.data.repository

import com.x0550.rubiccubesolver.data.preferences.OnboardingPreferences
import com.x0550.rubiccubesolver.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    private val onboardingPreferences: OnboardingPreferences
) : OnboardingRepository {
    override fun hasSeenScanInstructions(): Flow<Boolean> =
        onboardingPreferences.hasSeenScanInstructions

    override suspend fun markScanInstructionsSeen() {
        onboardingPreferences.markScanInstructionsSeen()
    }
}