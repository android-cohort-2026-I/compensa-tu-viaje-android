package com.compensatuviaje.tracker.feature.session.data

import android.content.Context
import android.content.SharedPreferences
import com.compensatuviaje.tracker.domain.SessionRepository
import com.compensatuviaje.tracker.domain.TokenStorage
import com.compensatuviaje.tracker.model.Session
import com.compensatuviaje.tracker.model.Truck
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import app.cash.turbine.test
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class FakeSharedPreferences : SharedPreferences {
    val map = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = map
    override fun getString(key: String, defValue: String?): String? = map[key] as? String ?: defValue
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = map[key] as? Set<String> ?: defValues
    override fun getInt(key: String, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor(this)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class FakeEditor(private val prefs: FakeSharedPreferences) : SharedPreferences.Editor {
        private val tempMap = mutableMapOf<String, Any?>()
        private val removes = mutableSetOf<String>()

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            tempMap[key] = value
            removes.remove(key)
            return this
        }
        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
            tempMap[key] = values
            removes.remove(key)
            return this
        }
        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            tempMap[key] = value
            removes.remove(key)
            return this
        }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            tempMap[key] = value
            removes.remove(key)
            return this
        }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            tempMap[key] = value
            removes.remove(key)
            return this
        }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            tempMap[key] = value
            removes.remove(key)
            return this
        }
        override fun remove(key: String): SharedPreferences.Editor {
            removes.add(key)
            tempMap.remove(key)
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            removes.addAll(prefs.map.keys)
            tempMap.clear()
            return this
        }
        override fun commit(): Boolean {
            apply()
            return true
        }
        override fun apply() {
            for (key in removes) {
                prefs.map.remove(key)
            }
            for ((key, value) in tempMap) {
                prefs.map[key] = value
            }
        }
    }
}

class FakeContext(private val prefs: SharedPreferences) : android.content.ContextWrapper(null) {
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
        return prefs
    }
    override fun getApplicationContext(): Context {
        return this
    }
}

class SessionRepositoryImplTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var context: Context
    private lateinit var tokenStorage: TokenStorage
    private lateinit var repository: SessionRepositoryImpl

    @Before
    fun setup() {
        fakePrefs = FakeSharedPreferences()
        context = FakeContext(fakePrefs)

        tokenStorage = TokenStorageImpl(context)
        repository = SessionRepositoryImpl(context, tokenStorage)
    }

    @Test
    fun `tokenStorage save get clear cycle works`() {
        assertThat(tokenStorage.get()).isNull()

        tokenStorage.save("dummy-token-123")
        assertThat(tokenStorage.get()).isEqualTo("dummy-token-123")

        tokenStorage.clear()
        assertThat(tokenStorage.get()).isNull()
    }

    @Test
    fun `isLoggedIn returns correct status`() = runTest {
        assertThat(repository.isLoggedIn()).isFalse()

        val session = Session(
            token = "dummy-token",
            driverName = "Juan Pérez",
            truck = Truck("truck-1", "ABC-123", "Semirremolque")
        )

        repository.setSession(session)
        assertThat(repository.isLoggedIn()).isTrue()

        repository.logout()
        assertThat(repository.isLoggedIn()).isFalse()
    }

    @Test
    fun `session data persists between instances`() = runTest {
        val session = Session(
            token = "persist-token",
            driverName = "María Gómez",
            truck = Truck("truck-2", "XYZ-789", "Cisterna")
        )

        // Save session in first repository instance
        repository.setSession(session)

        // Re-instantiate repository reading from the same context/tokenStorage
        val repo2 = SessionRepositoryImpl(context, tokenStorage)

        val restoredSession = repo2.current.first()
        assertThat(restoredSession).isNotNull()
        assertThat(restoredSession?.token).isEqualTo("persist-token")
        assertThat(restoredSession?.driverName).isEqualTo("María Gómez")
        assertThat(restoredSession?.truck?.id).isEqualTo("truck-2")
        assertThat(restoredSession?.truck?.licensePlate).isEqualTo("XYZ-789")
        assertThat(restoredSession?.truck?.category).isEqualTo("Cisterna")
    }

    @Test
    fun `current Flow emits null on logout`() = runTest {
        val session = Session(
            token = "logout-token",
            driverName = "Carlos Ruiz",
            truck = Truck("truck-3", "LMN-456", "Remolcador")
        )

        repository.setSession(session)

        repository.current.test {
            assertThat(awaitItem()).isEqualTo(session)
            repository.logout()
            assertThat(awaitItem()).isNull()
        }
    }
}
