package io.github.hylkeb.susstatemachine.test.requeststate

import io.github.hylkeb.susstatemachine.StateImpl
import io.github.hylkeb.susstatemachine.Transition
import kotlinx.coroutines.awaitCancellation

class SuccessStateImpl(override val response: String) : StateImpl<RequestState>(), RequestState.Success {
    override suspend fun enter(): Transition<RequestState> {
        // Terminal state, remain in the success state, keeping hold
        // of the response until the state machine is cancelled.
        // Possible improvement is a refresh method and a refreshing state
        // but that's not needed for testing purposes.
        awaitCancellation()
    }
}
