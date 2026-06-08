package com.compensatuviaje.tracker.feature.session

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.compensatuviaje.tracker.domain.SessionRepository
import com.compensatuviaje.tracker.domain.TokenStorage
import com.compensatuviaje.tracker.model.Session
import com.compensatuviaje.tracker.model.Truck
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionModule

class EncryptedTokenStorage(context: Context) : TokenStorage {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_token_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun save(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    override fun get(): String? = prefs.getString(KEY_TOKEN, null)

    override fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    companion object {
        private const val KEY_TOKEN = "jwt_token"
    }
}

class InMemorySessionRepository(
    private val tokenStorage: TokenStorage,
) : SessionRepository {

    private val _current = MutableStateFlow<Session?>(null)
    override val current: Flow<Session?> = _current.asStateFlow()

    override suspend fun setSession(session: Session) {
        tokenStorage.save(session.token)
        _current.value = session
    }

    override suspend fun logout() {
        tokenStorage.clear()
        _current.value = null
    }
}
