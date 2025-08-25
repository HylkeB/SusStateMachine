package io.github.hylkeb.susstatemachine.sample.requeststate

import io.github.hylkeb.susstatemachine.Transition
import io.github.hylkeb.susstatemachine.sample.DependencyContainer
import io.github.hylkeb.susstatemachine.sample.OpenForMocking
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job

@OpenForMocking
class Error(
    private val dependencyContainer: DependencyContainer,
    val cause: Throwable,
) : RequestState() {

    private val retryRequested: CompletableJob = Job()

    override suspend fun enter(): Transition<RequestState> {
        retryRequested.join()
        return Transition(Fetching(dependencyContainer))
    }

    suspend fun retry() {
        retryRequested.complete()
    }
}
