package io.github.hylkeb.susstatemachine.test

import io.github.hylkeb.susstatemachine.StateMachine
import io.github.hylkeb.susstatemachine.test.requeststate.RequestState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

/**
 * Sample usage of a state machine, in this case to manage a request, so only one fetch is running
 * at the same time, and multiple consumers of a certain request can reuse the running apicall and
 * api result.
 */
class RequestManager(di: DI) : DIAware by di {
    val stateMachine: StateMachine<RequestState> by instance()
//    val stateMachine: StateMachine<RequestState> = StateMachineImpl(IdleStateImpl(di), stateMachineName, stateObserver)

    suspend fun run() {
        stateMachine.run()
    }

    suspend fun getResponse(): Result<String> {
        when (val currentState = stateMachine.stateFlow.first()) {
            is RequestState.Success -> return Result.success(currentState.response) // reuse successful response directly
            is RequestState.Idle -> currentState.fetch() // first attempt at getting the response, start fetching
            is RequestState.Fetching -> { /*No direct action needed, will observe states instead*/ }
            is RequestState.Error -> currentState.retry() // new attempt at getting the response, retry the request
        }
        return stateMachine.stateFlow
            .mapNotNull { state ->
                when (state) {
                    is RequestState.Success -> Result.success(state.response)
                    is RequestState.Fetching -> null // still fetching, wait until next state transition
                    is RequestState.Error -> Result.failure(state.cause)
                    is RequestState.Idle -> Result.failure(IllegalStateException("RequestState should not be Initial here, was the state overridden?"))
                }
            }
            .first()
    }
}