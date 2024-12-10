package io.github.hylkeb.susstatemachine.sample

import io.github.hylkeb.susstatemachine.StateMachine
import io.github.hylkeb.susstatemachine.StateObserver
import io.github.hylkeb.susstatemachine.sample.requeststate.Error
import io.github.hylkeb.susstatemachine.sample.requeststate.Fetching
import io.github.hylkeb.susstatemachine.sample.requeststate.Idle
import io.github.hylkeb.susstatemachine.sample.requeststate.RequestState
import io.github.hylkeb.susstatemachine.sample.requeststate.Success
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull

/**
 * Sample usage of a state machine, in this case to manage a request, so only one fetch is running
 * at the same time, and multiple consumers of a certain request can reuse the running apicall and
 * api result.
 */
class RequestManager internal constructor(dependencyContainer: DependencyContainer) {

    public constructor(
        api: Api,
        requestStateObserver: StateObserver
    ) : this(RealDependencyContainer(api, requestStateObserver))

    val stateMachine: StateMachine<RequestState> by lazy { dependencyContainer.requestStateMachine }

    suspend fun run() {
        stateMachine.run()
    }

    suspend fun getResponse(): Result<String> {
        when (val currentState = stateMachine.stateFlow.first()) {
            is Success -> return Result.success(currentState.response) // reuse successful response directly
            is Idle -> currentState.fetch() // first attempt at getting the response, start fetching
            is Fetching -> { /*No direct action needed, will observe states instead*/ }
            is Error -> currentState.retry() // new attempt at getting the response, retry the request
        }
        return stateMachine.stateFlow
            .mapNotNull { state ->
                when (state) {
                    is Success -> Result.success(state.response)
                    is Fetching -> null // still fetching, wait until next state transition
                    is Error -> Result.failure(state.cause)
                    is Idle -> Result.failure(IllegalStateException("RequestState should not be Idle here, was the state overridden?"))
                }
            }
            .first()
    }
}