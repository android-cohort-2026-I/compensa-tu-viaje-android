package com.compensatuviaje.tracker.feature.session.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.compensatuviaje.tracker.domain.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class TokenStorageImpl(
    private val context: Context
) : TokenStorage {

    private val sharedPreferences: SharedPreferences by lazy {
        runBlocking(Dispatchers.IO) {
            try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    "secure_session_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                context.getSharedPreferences("secure_session_prefs_test", Context.MODE_PRIVATE)
            }
        }
    }

    override fun save(token: String) {
        sharedPreferences.edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    override fun get(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    override fun clear() {
        sharedPreferences.edit()
            .remove(KEY_TOKEN)
            .apply()
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
    }
}
