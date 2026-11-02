package com.x0550.rubiccubesolver.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.x0550.rubiccubesolver.data.analyzer.CubeFrameAnalyzer
import com.x0550.rubiccubesolver.data.repository.OnboardingRepositoryImpl
import com.x0550.rubiccubesolver.domain.engine.CubeSolverRepository
import com.x0550.rubiccubesolver.domain.engine.OfflineTwoPhaseSolver
import com.x0550.rubiccubesolver.domain.repository.OnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SolverModule {
    @Binds
    @Singleton
    abstract fun bindCubeSolver(impl: OfflineTwoPhaseSolver): CubeSolverRepository

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideCubeFrameAnalyzer(): CubeFrameAnalyzer = CubeFrameAnalyzer()

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("onboarding.preferences_pb") }
    )
}