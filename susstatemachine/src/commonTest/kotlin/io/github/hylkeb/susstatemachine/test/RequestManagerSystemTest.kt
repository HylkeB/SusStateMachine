package io.github.hylkeb.susstatemachine.test

import dev.mokkery.answering.calls
import dev.mokkery.everySuspend
import dev.mokkery.matcher.matching
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.github.hylkeb.susstatemachine.StateObserver
import io.github.hylkeb.susstatemachine.sample.Api
import io.github.hylkeb.susstatemachine.sample.DependencyContainer
import io.github.hylkeb.susstatemachine.sample.RealDependencyContainer
import io.github.hylkeb.susstatemachine.sample.RequestManager
import io.github.hylkeb.susstatemachine.sample.requeststate.Error
import io.github.hylkeb.susstatemachine.sample.requeststate.Fetching
import io.github.hylkeb.susstatemachine.sample.requeststate.Idle
import io.github.hylkeb.susstatemachine.sample.requeststate.RequestState
import io.github.hylkeb.susstatemachine.sample.requeststate.Success
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest

/**
 * This class tests the StateMachine implementation as it's used in a sample RequestManager class
 * Only the outermost dependences are mocked (the Api and the StateObserver).
 *
 * > Note: normally this would be redundant as it's split up in multiple smaller scoped unit tests.
 * > The states themselves should be individually tested, this is where the Api etc. should be mocked.
 * > The RequestManager should mock the StateMachine and test every possible state transition like that.
 *
 * @see [RequestManagerUnitTest]
 * @see [io.github.hylkeb.susstatemachine.test.requeststate.IdleUnitTest]
 * @see [io.github.hylkeb.susstatemachine.test.requeststate.ErrorUnitTest]
 * @see [io.github.hylkeb.susstatemachine.test.requeststate.FetchingUnitTest]
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RequestManagerSystemTest {

    private val api = mock<Api>()

    private val stateObserver = mock<StateObserver>()

    @Test
    fun testHappyPath() = runTest {
        // Arrange
        everySuspend { api.doApiCall() } calls {
            delay(100)
            Result.success("mocked response")
        }

        // Act
        val sut = RequestManager(api, stateObserver, backgroundScope)
        val observedStates = mutableListOf<RequestState>()
        val collectJob = launch { sut.stateMachine.stateFlow.toCollection(observedStates) }
        val result = sut.getResponse()
        advanceUntilIdle()

        collectJob.cancel()

        // Assert
        currentTime.shouldBe(100)
        observedStates.size.shouldBe(3)
        observedStates[0].shouldBeInstanceOf<Idle>()
        observedStates[1].shouldBeInstanceOf<Fetching>()
        observedStates[2].shouldBeInstanceOf<Success>()
            .response.shouldBe("mocked response")
        result.isSuccess.shouldBeTrue()
        result.getOrNull().shouldBe("mocked response")

        // Verify
        verifySuspend {
            stateObserver.stateTransition("request-state-machine", "Idle", "Fetching", null, null)
            api.doApiCall()
            stateObserver.stateTransition("request-state-machine", "Fetching", "Success", null, null)
        }
    }

    @Test
    fun testRetry() = runTest {
        // Arrange
        val mockedResponses = ArrayDeque<(suspend () -> Result<String>)>(listOf(
            {
                delay(100)
                Result.failure(Exception("mocked exception"))
            },
            {
                delay(100)
                Result.success("mocked response")
            },
        ))
        everySuspend { api.doApiCall() } calls {
            mockedResponses.removeFirst().invoke()
        }

        // Act
        val sut = RequestManager(api, stateObserver, backgroundScope)
        val observedStates = mutableListOf<RequestState>()
        val collectJob = launch { sut.stateMachine.stateFlow.toCollection(observedStates) }
        val result1 = sut.getResponse()
        val result2 = sut.getResponse()
        advanceUntilIdle()

        collectJob.cancel()

        // Assert
        currentTime.shouldBe(200)
        observedStates.size.shouldBe(5)
        observedStates[0].shouldBeInstanceOf<Idle>()
        observedStates[1].shouldBeInstanceOf<Fetching>()
        observedStates[2].shouldBeInstanceOf<Error>()
            .cause.message.shouldBe("mocked exception")
        observedStates[3].shouldBeInstanceOf<Fetching>()
        observedStates[4].shouldBeInstanceOf<Success>()
            .response.shouldBe("mocked response")

        result1.isSuccess.shouldBeFalse()
        result1.exceptionOrNull().shouldNotBeNull()
            .message.shouldBe("mocked exception")

        result2.isSuccess.shouldBeTrue()
        result2.getOrNull().shouldBe("mocked response")

        // Verify
        verifySuspend {
            stateObserver.stateTransition("request-state-machine", "Idle", "Fetching", null, null)
            api.doApiCall()
            stateObserver.stateTransition(
                stateMachine = ("request-state-machine"),
                fromState = ("Fetching"),
                toState = ("Error"),
                reason = ("api error"),
                cause = matching {
                    it.shouldNotBeNull()
                        .message.shouldBe("mocked exception")
                    true
                }
            )
            stateObserver.stateTransition("request-state-machine", "Error", "Fetching", null, null)
            api.doApiCall()
            stateObserver.stateTransition("request-state-machine", "Fetching", "Success", null, null)
        }
    }

    @Test
    fun testEnsureApiCalledJustOnceWithMultipleConsumers() = runTest {
        // Arrange
        everySuspend { api.doApiCall() } calls {
            delay(100)
            Result.success("mocked response")
        }

        // Act
        val sut = RequestManager(api, stateObserver, backgroundScope)
        val observedStates = mutableListOf<RequestState>()
        val collectJob = launch { sut.stateMachine.stateFlow.toCollection(observedStates) }
        val result1Deferred = async { sut.getResponse() }
        advanceTimeBy(20)
        val result2Deferred = async { sut.getResponse() }
        advanceTimeBy(20)
        val result3Deferred = async { sut.getResponse() }
        advanceTimeBy(20)
        val result4Deferred = async { sut.getResponse() }

        val result1 = result1Deferred.await()
        val result2 = result2Deferred.await()
        val result3 = result3Deferred.await()
        val result4 = result4Deferred.await()
        advanceTimeBy(10)
        val result5 = sut.getResponse()
        collectJob.cancel()

        // Assert
        currentTime.shouldBe(110) // 100 for the apicall + 10 ms later result5 is requested
        observedStates.size.shouldBe(3)
        observedStates[0].shouldBeInstanceOf<Idle>()
        observedStates[1].shouldBeInstanceOf<Fetching>()
        observedStates[2].shouldBeInstanceOf<Success>()
            .response.shouldBe("mocked response")
        result1.isSuccess.shouldBeTrue()
        result1.getOrNull().shouldBe("mocked response")
        result2.isSuccess.shouldBeTrue()
        result2.getOrNull().shouldBe("mocked response")
        result3.isSuccess.shouldBeTrue()
        result3.getOrNull().shouldBe("mocked response")
        result4.isSuccess.shouldBeTrue()
        result4.getOrNull().shouldBe("mocked response")
        result5.isSuccess.shouldBeTrue()
        result5.getOrNull().shouldBe("mocked response")

        // Verify
        verifySuspend {
            stateObserver.stateTransition("request-state-machine", "Idle", "Fetching", null, null)
            api.doApiCall()
            stateObserver.stateTransition("request-state-machine", "Fetching", "Success", null, null)
        }
    }

}