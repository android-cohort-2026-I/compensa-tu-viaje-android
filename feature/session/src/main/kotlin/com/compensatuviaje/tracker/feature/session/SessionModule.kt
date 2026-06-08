package com.compensatuviaje.tracker.feature.session

import android.content.Context
import com.compensatuviaje.tracker.domain.SessionRepository
import com.compensatuviaje.tracker.domain.TokenStorage

object SessionModule {
    private var _tokenStorage: TokenStorage? = null
    val tokenStorage: TokenStorage
        get() = _tokenStorage ?: throw IllegalStateException("SessionModule not initialized")

    private var _sessionRepository: SessionRepository? = null
    val sessionRepository: SessionRepository
        get() = _sessionRepository ?: throw IllegalStateException("SessionModule not initialized")

    fun init(context: Context) {
        // Will initialize implementations in subsequent commits
    }
}
