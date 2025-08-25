package io.github.hylkeb.susstatemachine.test.requeststate

import io.github.hylkeb.susstatemachine.sample.MockDependencyContainer
import io.github.hylkeb.susstatemachine.sample.requeststate.Fetching
import io.github.hylkeb.susstatemachine.sample.requeststate.Idle
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest

class IdleUnitTest {

    private val mockDependencyContainer = MockDependencyContainer()

    @Test
    fun enterWhenFetchCalledAssertTransitionToFetching() = runTest {
        // Arrange
        val sut = Idle(mockDependencyContainer)

        // Act
        val transition = async { sut.enter() }
        sut.fetch()

        // Assert
        transition.await().let {
            it.cause.shouldBeNull()
            it.reason.shouldBeNull()
            it.toState.shouldBeInstanceOf<Fetching>()
        }
    }
}