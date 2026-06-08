package com.compensatuviaje.tracker.feature.session.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.compensatuviaje.tracker.domain.SessionRepository
import com.compensatuviaje.tracker.domain.TokenStorage
import com.compensatuviaje.tracker.model.Session
import com.compensatuviaje.tracker.model.Truck
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class SessionRepositoryImpl(
    private val context: Context,
    private val tokenStorage: TokenStorage
) : SessionRepository {

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

    private val _current = MutableStateFlow<Session?>(null)
    override val current: Flow<Session?> = _current.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val token = tokenStorage.get()
        if (token != null) {
            val driverName = sharedPreferences.getString(KEY_DRIVER_NAME, null)
            val truckId = sharedPreferences.getString(KEY_TRUCK_ID, null)
            val licensePlate = sharedPreferences.getString(KEY_TRUCK_LICENSE_PLATE, null)
            val category = sharedPreferences.getString(KEY_TRUCK_CATEGORY, null)

            if (driverName != null && truckId != null && licensePlate != null && category != null) {
                _current.value = Session(
                    token = token,
                    driverName = driverName,
                    truck = Truck(truckId, licensePlate, category)
                )
            }
        }
    }

    override suspend fun setSession(session: Session) {
        tokenStorage.save(session.token)
        sharedPreferences.edit()
            .putString(KEY_DRIVER_NAME, session.driverName)
            .putString(KEY_TRUCK_ID, session.truck.id)
            .putString(KEY_TRUCK_LICENSE_PLATE, session.truck.licensePlate)
            .putString(KEY_TRUCK_CATEGORY, session.truck.category)
            .apply()

        _current.value = session
    }

    override suspend fun logout() {
        tokenStorage.clear()
        sharedPreferences.edit()
            .remove(KEY_DRIVER_NAME)
            .remove(KEY_TRUCK_ID)
            .remove(KEY_TRUCK_LICENSE_PLATE)
            .remove(KEY_TRUCK_CATEGORY)
            .apply()

        _current.value = null
    }

    fun isLoggedIn(): Boolean {
        return tokenStorage.get() != null
    }

    suspend fun onUnauthorized() {
        logout()
    }

    companion object {
        private const val KEY_DRIVER_NAME = "driver_name"
        private const val KEY_TRUCK_ID = "truck_id"
        private const val KEY_TRUCK_LICENSE_PLATE = "truck_license_plate"
        private const val KEY_TRUCK_CATEGORY = "truck_category"
    }
}
