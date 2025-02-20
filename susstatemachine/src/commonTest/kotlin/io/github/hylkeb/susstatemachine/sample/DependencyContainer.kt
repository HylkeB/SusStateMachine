package io.github.hylkeb.susstatemachine.sample

import io.github.hylkeb.susstatemachine.StateMachine
import io.github.hylkeb.susstatemachine.StateObserver
import io.github.hylkeb.susstatemachine.sample.requeststate.Idle
import io.github.hylkeb.susstatemachine.sample.requeststate.RequestState

interface DependencyContainer {
    val stateObserver: StateObserver?
    val requestStateMachine: StateMachine<RequestState>
    val api: Api
}

class RealDependencyContainer(
    override val api: Api,
    override val stateObserver: StateObserver?,
) : DependencyContainer {
    override val requestStateMachine: StateMachine<RequestState> by lazy {
        StateMachine(Idle(this), "request-state-machine", stateObserver)
    }
}

class MockDependencyContainer(
    private val _stateObserver: StateObserver? = null,
    private val _requestStateMachine: StateMachine<RequestState>? = null,
    private val _api: Api? = null,
) : DependencyContainer {
    override val stateObserver: StateObserver? get() = _stateObserver
    override val requestStateMachine: StateMachine<RequestState>
        get() = requireNotNull(_requestStateMachine) { "No mocked value provided for requestStateMachine" }
    override val api: Api
        get() = requireNotNull(_api) { "No mocked value provided for api" }

}