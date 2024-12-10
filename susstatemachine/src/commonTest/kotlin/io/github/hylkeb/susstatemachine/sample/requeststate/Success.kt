package io.github.hylkeb.susstatemachine.sample.requeststate

import io.github.hylkeb.susstatemachine.Transition
import io.github.hylkeb.susstatemachine.sample.OpenForMocking
import kotlinx.coroutines.awaitCancellation

@OpenForMocking
class Success(val response: String) : RequestState() {
    override suspend fun enter(): Transition<RequestState> {
        // Terminal state, remain in the success state, keeping hold
        // of the response until the state machine is cancelled.
        // Possible improvement is a refresh method and a refreshing state
        // but that's not needed for testing purposes.
        awaitCancellation()
    }
}
