package com.compensatuviaje.tracker.feature.session

import com.compensatuviaje.tracker.domain.TokenStorage
import com.compensatuviaje.tracker.model.Session
import com.compensatuviaje.tracker.model.Truck
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class FakeTokenStorage : TokenStorage {
    private var token: String? = null
    override fun save(token: String) { this.token = token }
    override fun get(): String? = token
    override fun clear() { token = null }
}

class SessionModuleTest {

    private lateinit var tokenStorage: FakeTokenStorage
    private lateinit var sessionRepo: InMemorySessionRepository

    private val sampleSession = Session(
        token = "jwt-test-token-123",
        driverName = "Diego Huacca",
        truck = Truck(id = "truck-1", licensePlate = "ABC-123", category = "Camion"),
    )

    @Before
    fun setUp() {
        tokenStorage = FakeTokenStorage()
        sessionRepo = InMemorySessionRepository(tokenStorage)
    }

    @Test
    fun `save token stores value`() {
        tokenStorage.save("my-token")
        assertThat(tokenStorage.get()).isEqualTo("my-token")
    }

    @Test
    fun `clear token removes value`() {
        tokenStorage.save("my-token")
        tokenStorage.clear()
        assertThat(tokenStorage.get()).isNull()
    }

    @Test
    fun `setSession saves token and emits session`() = runTest {
        sessionRepo.setSession(sampleSession)
        assertThat(tokenStorage.get()).isEqualTo("jwt-test-token-123")
        assertThat(sessionRepo.current.first()).isEqualTo(sampleSession)
    }

    @Test
    fun `logout clears token and emits null`() = runTest {
        sessionRepo.setSession(sampleSession)
        sessionRepo.logout()
        assertThat(tokenStorage.get()).isNull()
        assertThat(sessionRepo.current.first()).isNull()
    }

    @Test
    fun `initial session is null`() = runTest {
        assertThat(sessionRepo.current.first()).isNull()
    }
}
