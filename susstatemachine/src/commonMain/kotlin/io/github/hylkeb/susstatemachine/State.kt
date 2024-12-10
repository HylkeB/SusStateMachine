package io.github.hylkeb.susstatemachine

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first

/**
 * This class should be implemented by a concrete set of states on which a [StateMachine] operates.
 * Ideally that should be performed by defining a sealed class of the StateType, which every concrete state implements.
 */
abstract class State<T : State<T>> {

    /**
     * This property will be completed with the stateFlow owned by the owning StateMachine,
     * which can then be used to for [awaitTransition].
     * It is nullable so it can be set to null in case of testing states standalone, see [prepareForTest][io.github.hylkeb.susstatemachine.test.prepareForTest].
     */
    private val stateMachine: CompletableDeferred<StateMachine<*>?> = CompletableDeferred()

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
    abstract suspend fun enter(): Transition<T>

    /**
     * This method suspends until this state is no longer the active state of the [StateMachine].
     * Useful when an external event on a state should yield a state transition before returning.
     *
     * See example documentation in [State] for more explanation on this.
     * Specifically the Idle state and te getApiResult method.
     *
     * For unit testing states that use [awaitTransition], prepare the state under test using [prepareForTest][io.github.hylkeb.susstatemachine.test.prepareForTest].
     */
    protected suspend fun awaitTransition() {
        stateMachine.await()?.stateFlow?.first { it !== this }
    }

    /**
     * Called when the state became active in the given StateMachine, or null when the state is unit tested and
     * [prepareForTest][io.github.hylkeb.susstatemachine.test.prepareForTest] is called.
     */
    internal fun becameActive(stateMachine: StateMachine<*>?) {
        this.stateMachine.complete(stateMachine)
    }
}
