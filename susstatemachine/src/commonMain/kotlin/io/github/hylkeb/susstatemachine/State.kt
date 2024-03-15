package io.github.hylkeb.susstatemachine

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first

interface State<T : State<T>> {

    /**
     * Name of the state, used primarily for debug purposes.
     * Can be overridden by child classes, e.g. to circumvent obfuscation.
     */
    val name: String get() = this::class.simpleName.toString()

    /**
     * Called when the [StateMachine] enters this state.
     * This suspendable method should perform actions required by this state, and
     * suspend until it's ready to transition to a new state.
     * @return A [Transition] towards a followup state.
     */
    suspend fun enter(): Transition<T>

    /**
     * Called when the [StateMachine] just made this state the active state.
     * @param stateMachine The StateMachine for which this state is the active state.
     */
    fun becameActive(stateMachine: StateMachine<*>) { }
}

/**
 * This class should be implemented by a concrete set of states on which a [StateMachine] operates.
 * Ideally that should be performed by defining a sealed class of the StateType, which every concrete state implements.
 */
abstract class StateImpl<T : State<T>> : State<T> {
    private val stateMachine: CompletableDeferred<StateMachine<*>> = CompletableDeferred()

    /**
     * This method suspends until this state is no longer the active state of the [StateMachine].
     * Useful when an external event on a state should yield a state transition before returning.
     *
     * See example documentation in [State] for more explanation on this.
     * Specifically the Idle state and te getApiResult method.
     */
    protected suspend fun awaitTransition() {
        stateMachine.await().stateFlow.first { it !== this }
    }

    override fun becameActive(stateMachine: StateMachine<*>) {
        this.stateMachine.complete(stateMachine)
    }
}
