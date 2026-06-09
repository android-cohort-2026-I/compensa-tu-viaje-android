package com.compensatuviaje.tracker.feature.auth

import com.compensatuviaje.tracker.domain.AppResult
import com.compensatuviaje.tracker.domain.ErrorKind
import com.compensatuviaje.tracker.domain.SessionRepository
import com.compensatuviaje.tracker.domain.TokenStorage
import com.compensatuviaje.tracker.model.Session
import com.compensatuviaje.tracker.testing.FakeMobileApi
import com.compensatuviaje.tracker.testing.SampleData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

// Fakes locales para los test
class FakeTokenStorage : TokenStorage {
    private var token: String? = null
    override fun save(token: String) { this.token = token }
    override fun get(): String? = token
    override fun clear() { token = null }
}

class FakeSessionRepository : SessionRepository {
    private val session = MutableStateFlow<Session?>(null)
    override val current: Flow<Session?> = session
    override suspend fun setSession(session: Session) { this.session.value = session }
    override suspend fun logout() { this.session.value = null }
}

// Tests del ViewModel
class AuthViewModelTest {

    private lateinit var fakeApi: FakeMobileApi
    private lateinit var fakeTokenStorage: FakeTokenStorage
    private lateinit var fakeSessionRepo: FakeSessionRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())

        fakeApi = FakeMobileApi()
        fakeTokenStorage = FakeTokenStorage()
        fakeSessionRepo = FakeSessionRepository()
        viewModel = AuthViewModel(fakeApi, fakeTokenStorage, fakeSessionRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loginExitoso() = runTest {
        fakeApi.loginResult = AppResult.Ok(SampleData.session)

        viewModel.onDriverIdChange("chofer-01")
        viewModel.onLicensePlateChange("XYZ-987")
        viewModel.onPinChange("1234")

        viewModel.onLoginClick()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoginSuccess).isTrue()
        assertThat(state.errorMessage).isNull()
        assertThat(fakeTokenStorage.get()).isEqualTo(SampleData.session.token)
    }

    @Test
    fun loginFallido() = runTest {
        fakeApi.loginResult = AppResult.Err(ErrorKind.UNAUTHORIZED)

        viewModel.onDriverIdChange("chofer-01")
        viewModel.onLicensePlateChange("XYZ-987")
        viewModel.onPinChange("0000")

        viewModel.onLoginClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoginSuccess).isFalse()
        assertThat(state.errorMessage).isEqualTo("Credenciales inválidas")
    }

    @Test
    fun loginSinRed() = runTest {
        fakeApi.loginResult = AppResult.Err(ErrorKind.NETWORK)

        viewModel.onDriverIdChange("chofer-01")
        viewModel.onLicensePlateChange("XYZ-987")
        viewModel.onPinChange("1234")

        viewModel.onLoginClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoginSuccess).isFalse()
        assertThat(state.errorMessage).isEqualTo("Sin conexión a internet")
    }
}

