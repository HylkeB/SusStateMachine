package io.github.hylkeb.susstatemachine.test.requeststate

import io.github.hylkeb.susstatemachine.StateImpl
import io.github.hylkeb.susstatemachine.Transition
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import org.kodein.di.DI
import org.kodein.di.DIAware

class IdleStateImpl(di: DI) : StateImpl<RequestState>(), RequestState.Idle, DIAware by di {

    private val fetchRequested: CompletableJob = Job()

    override suspend fun enter(): Transition<RequestState> {
        fetchRequested.join()
        return Transition(FetchingStateImpl(di))
    }

    override suspend fun fetch() {
        fetchRequested.complete()
        awaitTransition()
    }
}