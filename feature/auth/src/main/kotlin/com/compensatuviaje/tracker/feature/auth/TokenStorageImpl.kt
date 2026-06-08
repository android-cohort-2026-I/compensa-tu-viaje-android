package com.compensatuviaje.tracker.feature.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.compensatuviaje.tracker.domain.TokenStorage

class TokenStorageImpl(context: Context) : TokenStorage {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun save(token: String) {
        sharedPreferences.edit().putString("JWT_TOKEN", token).apply()
    }

    override fun get(): String? {
        return sharedPreferences.getString("JWT_TOKEN", null)
    }

    override fun clear() {
        sharedPreferences.edit().remove("JWT_TOKEN").apply()
    }
}