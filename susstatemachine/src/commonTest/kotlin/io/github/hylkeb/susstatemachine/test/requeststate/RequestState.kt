package io.github.hylkeb.susstatemachine.test.requeststate

import io.github.hylkeb.susstatemachine.State

sealed interface RequestState : State<RequestState> {

    /**
     * The Idle state will wait for an external trigger to transition to the Fetching state.
     */
    interface Idle : RequestState {
        suspend fun fetch()
    }

    /**
     * Uses the API to fetch the data
     */
    interface Fetching : RequestState

    /**
     * If the fetch failed, it will end up in the error state, in which it will remain
     * until a retry is attempted.
     */
    interface Error : RequestState {
        val cause: Throwable
        suspend fun retry()
    }

    /**
     * The success state, if the api call succeeded, it will remain in the success state,
     * and therefore keep track of the successful response, for as long as the statemachine is running.
     */
    interface Success : RequestState {
        val response: String
    }
}
