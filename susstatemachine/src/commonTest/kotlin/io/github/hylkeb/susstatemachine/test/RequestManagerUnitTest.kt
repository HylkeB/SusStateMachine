package io.github.hylkeb.susstatemachine.test

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.github.hylkeb.susstatemachine.StateMachine
import io.github.hylkeb.susstatemachine.sample.MockDependencyContainer
import io.github.hylkeb.susstatemachine.sample.RequestManager
import io.github.hylkeb.susstatemachine.sample.requeststate.Error
import io.github.hylkeb.susstatemachine.sample.requeststate.Fetching
import io.github.hylkeb.susstatemachine.sample.requeststate.Idle
import io.github.hylkeb.susstatemachine.sample.requeststate.RequestState
import io.github.hylkeb.susstatemachine.sample.requeststate.Success
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class RequestManagerUnitTest {
    private val requestStateMachine = mock<StateMachine<RequestState>>()
    private val idleState = mock<Idle>()
    private val fetchingState = mock<Fetching>()
    private val errorState = mock<Error>()
    private val successState = mock<Success>()
    private val dependencyContainer = MockDependencyContainer(
        _requestStateMachine = requestStateMachine,
    )

    @Test
    fun getResponseWhenStateTransitionsToErrorAssertFailure() = runTest {
        // Arrange
        val apiException = Exception("fake exception")
        val mockedFlow = MutableStateFlow<RequestState>(idleState)
        every { requestStateMachine.stateFlow } returns mockedFlow
        every { errorState.cause } returns apiException
        val sut = RequestManager(dependencyContainer, backgroundScope)

        // Act
        val result = async { sut.getResponse() }
        runCurrent()
        mockedFlow.emit(fetchingState)
        runCurrent()
        mockedFlow.emit(errorState)
        runCurrent()

        // Assert
        result.await().shouldBeFailure()
            .shouldBe(apiException)
        verifySuspend {
            idleState.fetch()
            errorState.cause
        }
    }

    @Test
    fun getResponseWhenStateTransitionsToSuccessAssertSuccess() = runTest {
        // Arrange
        val mockedFlow = MutableStateFlow<RequestState>(idleState)
        every { requestStateMachine.stateFlow } returns mockedFlow
        every { successState.response } returns "response"
        val sut = RequestManager(dependencyContainer, backgroundScope)

        // Act
        val result = async { sut.getResponse() }
        runCurrent()
        mockedFlow.emit(fetchingState)
        runCurrent()
        mockedFlow.emit(successState)
        runCurrent()

        // Assert
        verifySuspend {
            idleState.fetch()
            successState.response
        }
        result.await().shouldBeSuccess()
            .shouldBe("response")
    }

    @Test
    fun getResponseWhenStateAlreadyFetchingAssertResult() = runTest {
        // Arrange
        val mockedFlow = MutableStateFlow<RequestState>(fetchingState)
        every { requestStateMachine.stateFlow } returns mockedFlow
        every { successState.response } returns "response"
        val sut = RequestManager(dependencyContainer, backgroundScope)

        // Act
        val result = async { sut.getResponse() }
        runCurrent()
        mockedFlow.emit(successState)
        runCurrent()

        // Assert
        verifySuspend {
            successState.response
        }
        result.await().shouldBeSuccess()
            .shouldBe("response")
    }

    @Test
    fun getResponseWhenStateAlreadyErrorAssertRetryAndResult() = runTest {
        // Arrange
        val mockedFlow = MutableStateFlow<RequestState>(errorState)
        every { requestStateMachine.stateFlow } returns mockedFlow
        every { successState.response } returns "response"
        val sut = RequestManager(dependencyContainer, backgroundScope)

        // Act
        val result = async { sut.getResponse() }
        runCurrent()
        mockedFlow.emit(fetchingState)
        runCurrent()
        mockedFlow.emit(successState)
        runCurrent()

        // Assert
        verifySuspend {
            errorState.retry()
            successState.response
        }
        result.await().shouldBeSuccess()
            .shouldBe("response")
    }
}
