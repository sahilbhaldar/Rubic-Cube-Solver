package com.x0550.rubiccubesolver.domain.model

import androidx.annotation.StringRes

/**
 * A domain failure that the Android presentation layer can render in the active app locale.
 * The domain and solver remain usable from plain JVM tests without Android resource access.
 */
class LocalizedError(
    @StringRes val messageRes: Int,
    val formatArgs: List<Any> = emptyList()
) : IllegalArgumentException()