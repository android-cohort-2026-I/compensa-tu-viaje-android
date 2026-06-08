package com.compensatuviaje.tracker.feature.authfirebase

import app.cash.turbine.test
import com.compensatuviaje.tracker.domain.AppResult
import com.compensatuviaje.tracker.domain.ErrorKind
import com.compensatuviaje.tracker.model.Session
import com.compensatuviaje.tracker.testing.SampleData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Fake para el repositorio de autenticación.
 */
class FakeAuthRepository : AuthRepository {
    var result: AppResult<Session> = AppResult.Ok(SampleData.session)
    var logoutCalled = false
    var currentSession: Session? = null

    override suspend fun login(email: String, password: String) = result
    override suspend fun logout(): AppResult<Unit> {
        logoutCalled = true
        return AppResult.Ok(Unit)
    }
    override suspend fun getCurrentSession(): Session? = currentSession
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthFirebaseViewModelTest {

    private lateinit var viewModel: AuthFirebaseViewModel
    private lateinit var repository: FakeAuthRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeAuthRepository()
        viewModel = AuthFirebaseViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        assertThat(viewModel.uiState.value).isInstanceOf(AuthUiState.Idle::class.java)
    }

    @Test
    fun `login success updates state to Success`() = runTest {
        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(AuthUiState.Idle::class.java)
            
            viewModel.login("test@test.com", "1234")
            
            assertThat(awaitItem()).isInstanceOf(AuthUiState.Loading::class.java)
            val successState = awaitItem() as AuthUiState.Success
            assertThat(successState.session.token).isEqualTo(SampleData.session.token)
        }
    }

    @Test
    fun `login error updates state to Error`() = runTest {
        repository.result = AppResult.Err(ErrorKind.UNAUTHORIZED, "Credenciales inválidas")
        
        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(AuthUiState.Idle::class.java)
            
            viewModel.login("test@test.com", "wrong")
            
            assertThat(awaitItem()).isInstanceOf(AuthUiState.Loading::class.java)
            val errorState = awaitItem() as AuthUiState.Error
            assertThat(errorState.message).contains("Credenciales inválidas")
        }
    }

    @Test
    fun `empty credentials show error without calling repository`() = runTest {
        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(AuthUiState.Idle::class.java)
            
            viewModel.login("", "")
            
            val errorState = awaitItem() as AuthUiState.Error
            assertThat(errorState.message).contains("obligatorios")
        }
    }
}
