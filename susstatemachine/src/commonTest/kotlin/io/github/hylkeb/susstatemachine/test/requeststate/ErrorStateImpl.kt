package io.github.hylkeb.susstatemachine.test.requeststate

import io.github.hylkeb.susstatemachine.StateImpl
import io.github.hylkeb.susstatemachine.Transition
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import org.kodein.di.DI
import org.kodein.di.DIAware

class ErrorStateImpl(
    di: DI,
    override val cause: Throwable,
) : StateImpl<RequestState>(), RequestState.Error, DIAware by di {

    private val retryRequested: CompletableJob = Job()

    override suspend fun enter(): Transition<RequestState> {
        retryRequested.join()
        return Transition(FetchingStateImpl(di))
    }

    override suspend fun retry() {
        retryRequested.complete()
        awaitTransition()
    }
}
