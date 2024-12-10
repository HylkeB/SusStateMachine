package io.github.hylkeb.susstatemachine.test.requeststate

import dev.mokkery.answering.returnsFailure
import dev.mokkery.answering.returnsSuccess
import dev.mokkery.everySuspend
import dev.mokkery.mock
import io.github.hylkeb.susstatemachine.sample.Api
import io.github.hylkeb.susstatemachine.sample.MockDependencyContainer
import io.github.hylkeb.susstatemachine.sample.requeststate.Error
import io.github.hylkeb.susstatemachine.sample.requeststate.Fetching
import io.github.hylkeb.susstatemachine.sample.requeststate.Success
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class FetchingUnitTest {

    private val api = mock<Api>()

    private val mockDependencyContainer = MockDependencyContainer(
        _api = api
    )

    @Test
    fun enterWhenApiFailsAssertTransitionToError() = runTest {
        // Arrange
        val apiException = Exception("fake exception")
        everySuspend { api.doApiCall() } returnsFailure apiException
        val sut = Fetching(mockDependencyContainer)

        // Act
        val transition = sut.enter()

        // Assert
        transition.cause.shouldBe(apiException)
        transition.reason.shouldBe("api error")
        transition.toState.shouldBeInstanceOf<Error>()
            .cause.shouldBe(apiException)
    }

    @Test
    fun enterWhenApiSucceedsAssertTransitionToSuccess() = runTest {
        // Arrange
        everySuspend { api.doApiCall() } returnsSuccess "api response"
        val sut = Fetching(mockDependencyContainer)

        // Act
        val transition = sut.enter()

        // Assert
        transition.reason.shouldBeNull()
        transition.cause.shouldBeNull()
        transition.toState.shouldBeInstanceOf<Success>()
            .response.shouldBe("api response")
    }

}