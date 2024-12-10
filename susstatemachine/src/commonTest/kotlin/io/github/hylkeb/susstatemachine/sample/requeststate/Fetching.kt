package io.github.hylkeb.susstatemachine.sample.requeststate

import io.github.hylkeb.susstatemachine.Transition
import io.github.hylkeb.susstatemachine.sample.Api
import io.github.hylkeb.susstatemachine.sample.DependencyContainer
import io.github.hylkeb.susstatemachine.sample.OpenForMocking

@OpenForMocking
class Fetching(
    private val dependencyContainer: DependencyContainer,
) : RequestState() {

    private val api: Api by lazy { dependencyContainer.api }

    override suspend fun enter(): Transition<RequestState> {
        val response = api.doApiCall()
            .getOrElse { return Transition(Error(dependencyContainer, it), "api error", it) }
        return Transition(Success(response))
    }

}
