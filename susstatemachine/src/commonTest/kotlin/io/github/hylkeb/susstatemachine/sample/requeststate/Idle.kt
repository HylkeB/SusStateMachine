package io.github.hylkeb.susstatemachine.sample.requeststate

import io.github.hylkeb.susstatemachine.Transition
import io.github.hylkeb.susstatemachine.sample.DependencyContainer
import io.github.hylkeb.susstatemachine.sample.OpenForMocking
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job

@OpenForMocking
class Idle(private val dependencyContainer: DependencyContainer) : RequestState() {

    private val fetchRequested: CompletableJob = Job()

    override suspend fun enter(): Transition<RequestState> {
        fetchRequested.join()
        return Transition(Fetching(dependencyContainer))
    }

    suspend fun fetch() {
        fetchRequested.complete()
    }
}