package io.github.hylkeb.susstatemachine.test.requeststate

import io.github.hylkeb.susstatemachine.sample.MockDependencyContainer
import io.github.hylkeb.susstatemachine.sample.requeststate.Error
import io.github.hylkeb.susstatemachine.sample.requeststate.Fetching
import io.github.hylkeb.susstatemachine.test.prepareForTest
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest

class ErrorUnitTest {

    private val mockDependencyContainer = MockDependencyContainer()

    @Test
    fun enterWhenRetryCalledAssertTransitionToFetching() = runTest {
        // Arrange
        val sut = Error(mockDependencyContainer, Exception("fake exception")).prepareForTest() // Need to prepare the state for test, because it uses awaitTransition

        // Act
        val transition = async { sut.enter() }
        sut.retry()

        // Assert
        transition.await().let {
            it.cause.shouldBeNull()
            it.reason.shouldBeNull()
            it.toState.shouldBeInstanceOf<Fetching>()
        }
    }
}