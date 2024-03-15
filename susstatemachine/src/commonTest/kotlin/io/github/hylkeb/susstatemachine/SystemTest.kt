package io.github.hylkeb.susstatemachine

import io.github.hylkeb.susstatemachine.test.Api
import io.github.hylkeb.susstatemachine.test.RequestManager
import io.github.hylkeb.susstatemachine.test.requeststate.IdleStateImpl
import io.github.hylkeb.susstatemachine.test.requeststate.RequestState
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.mock.ArgConstraint
import org.kodein.mock.Mock
import org.kodein.mock.tests.TestsWithMocks

/**
 * This class tests the sample StateMachine implementation as its used in a sample RequestManager class
 * Only the outer most dependency is mocked (the Api).
 *
 * > Note: normally this would be redundant as its split up in multiple smaller scoped unit tests.
 * > The states themselves should be individually tested, this is where the Api etc should be mocked.
 * > The RequestManager should mock the StateMachine and test every possible state transition like that.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SystemTest : TestsWithMocks() {
    override fun setUpMocks() = injectMocks(mocker)

    @Mock
    lateinit var api: Api

    @Mock
    lateinit var stateObserver: StateObserver

    private val di: DI by withMocks {
        DI {
            bindSingleton { api }
            bindSingleton<StateMachine<RequestState>> { StateMachineImpl(IdleStateImpl(di), "RequestStateMachine", stateObserver) }
        }
    }

    private val sut: RequestManager by withMocks {
        RequestManager(di)
    }

    @Test
    fun testHappyPath() = runTest {
        // Arrange
        everySuspending { api.doApiCall() } runs {
            delay(100)
            Result.success("mocked response")
        }
        every { stateObserver.stateTransition(isAny(), isAny(), isAny(), isAny(), isAny()) } returns Unit

        // Act
        val runJob = launch { sut.run() }
        val observedStates = mutableListOf<RequestState>()
        val collectJob = launch { sut.stateMachine.stateFlow.toList(observedStates) }
        val result = sut.getResponse()

        runJob.cancel()
        collectJob.cancel()

        // Assert
        currentTime.shouldBe(100)
        observedStates.size.shouldBe(3)
        observedStates[0].shouldBeInstanceOf<RequestState.Idle>()
        observedStates[1].shouldBeInstanceOf<RequestState.Fetching>()
        observedStates[2].shouldBeInstanceOf<RequestState.Success>()
            .response.shouldBe("mocked response")
        result.isSuccess.shouldBeTrue()
        result.getOrNull().shouldBe("mocked response")

        // Verify
        verifyWithSuspend {
            stateObserver.stateTransition("RequestStateMachine", "IdleStateImpl", "FetchingStateImpl", null, null)
            api.doApiCall()
            stateObserver.stateTransition("RequestStateMachine", "FetchingStateImpl", "SuccessStateImpl", null, null)
        }
    }

    @Test
    fun testRetry() = runTest {
        // Arrange
        val mockedResponses = ArrayDeque<(suspend (Array<*>) -> Result<String>)>(listOf(
            {
                delay(100)
                Result.failure(Exception("mocked exception"))
            },
            {
                delay(100)
                Result.success("mocked response")
            },
        ))
        everySuspending { api.doApiCall() } runs {
            mockedResponses.removeFirst().invoke(it)
        }
        every { stateObserver.stateTransition(isAny(), isAny(), isAny(), isAny(), isAny()) } runs { println(it.joinToString()) }

        // Act
        val runJob = launch { sut.run() }
        val observedStates = mutableListOf<RequestState>()
        val collectJob = launch { sut.stateMachine.stateFlow.toList(observedStates) }
        val result1 = sut.getResponse()
        val result2 = sut.getResponse()

        runJob.cancel()
        collectJob.cancel()

        // Assert
        currentTime.shouldBe(200)
        observedStates.size.shouldBe(5)
        observedStates[0].shouldBeInstanceOf<RequestState.Idle>()
        observedStates[1].shouldBeInstanceOf<RequestState.Fetching>()
        observedStates[2].shouldBeInstanceOf<RequestState.Error>()
            .cause.message.shouldBe("mocked exception")
        observedStates[3].shouldBeInstanceOf<RequestState.Fetching>()
        observedStates[4].shouldBeInstanceOf<RequestState.Success>()
            .response.shouldBe("mocked response")

        result1.isSuccess.shouldBeFalse()
        result1.exceptionOrNull().shouldNotBeNull()
            .message.shouldBe("mocked exception")

        result2.isSuccess.shouldBeTrue()
        result2.getOrNull().shouldBe("mocked response")

        // Verify
        verifyWithSuspend {
            stateObserver.stateTransition("RequestStateMachine", "IdleStateImpl", "FetchingStateImpl", null, null)
            api.doApiCall()
            stateObserver.stateTransition(
                isEqual("RequestStateMachine"),
                isEqual("FetchingStateImpl"),
                isEqual("ErrorStateImpl"),
                isEqual("api error"),
                isValid {
                    it.shouldNotBeNull()
                        .message.shouldBe("mocked exception")
                    ArgConstraint.Result.Success
                }
            )
            stateObserver.stateTransition("RequestStateMachine", "ErrorStateImpl", "FetchingStateImpl", null, null)
            api.doApiCall()
            stateObserver.stateTransition("RequestStateMachine", "FetchingStateImpl", "SuccessStateImpl", null, null)
        }
    }

    @Test
    fun testEnsureApiCalledJustOnceWithMultipleConsumers() = runTest {
        // Arrange
        everySuspending { api.doApiCall() } runs {
            delay(100)
            Result.success("mocked response")
        }
        every { stateObserver.stateTransition(isAny(), isAny(), isAny(), isAny(), isAny()) } returns Unit

        // Act
        val runJob = launch { sut.run() }
        val observedStates = mutableListOf<RequestState>()
        val collectJob = launch { sut.stateMachine.stateFlow.toList(observedStates) }
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
        runJob.cancel()
        collectJob.cancel()

        // Assert
        currentTime.shouldBe(110) // 100 for the apicall + 10 ms later result5 is requested
        observedStates.size.shouldBe(3)
        observedStates[0].shouldBeInstanceOf<RequestState.Idle>()
        observedStates[1].shouldBeInstanceOf<RequestState.Fetching>()
        observedStates[2].shouldBeInstanceOf<RequestState.Success>()
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
        verifyWithSuspend {
            stateObserver.stateTransition("RequestStateMachine", "IdleStateImpl", "FetchingStateImpl", null, null)
            api.doApiCall()
            stateObserver.stateTransition("RequestStateMachine", "FetchingStateImpl", "SuccessStateImpl", null, null)
        }
    }

}