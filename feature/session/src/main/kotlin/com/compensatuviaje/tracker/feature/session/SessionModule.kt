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
        if (_tokenStorage == null || _sessionRepository == null) {
            val storage = com.compensatuviaje.tracker.feature.session.data.TokenStorageImpl(context)
            _tokenStorage = storage
            _sessionRepository = com.compensatuviaje.tracker.feature.session.data.SessionRepositoryImpl(context, storage)
        }
    }
}
