package io.github.hylkeb.susstatemachine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

/**
 * A [StateMachine] executes a concrete set of [State]s of type [T] and manages state transitions.
 *
 * When the state machine runs, it repeatedly performs the following steps as long as the
 * [CoroutineScope] is active:
 * 1. Call [State.enter] on the active state.
 * 2. Wait until a new [Transition] is returned.
 * 3. Notify the [StateObserver] (if present) that a transition is about to occur.
 * 4. Update the active state to [Transition.toState].
 * 5. Repeat from step 1.
 *
 * The active state can also be overridden via [overrideState], which cancels the current state
 * and immediately applies the provided transition.
 */
interface StateMachine<T : State<T>> {

    companion object {
        /**
         * Creates a new [StateMachine] instance.
         *
         * @param initialState The initial state to execute when the machine starts.
         * @param stateMachineName Optional name for the machine, used in [StateObserver] callbacks.
         * @param stateObserver Optional observer called on every state transition.
         */
        operator fun <T : State<T>> invoke(
            initialState: T,
            stateMachineName: String = "state-machine",
            stateObserver: StateObserver? = null,
        ): StateMachine<T> {
            return StateMachineImpl(
                initialState = initialState,
                stateMachineName = stateMachineName,
                stateObserver = stateObserver,
            )
        }

        /**
         * Suspends until the next state becomes active, starting from [currentState].
         *
         * @param currentState The state to wait for a transition away from.
         * @return The next active state after [currentState].
         */
        suspend inline fun <T : State<T>> StateMachine<T>.awaitNextState(currentState: T): T {
            return stateFlow.first { it != currentState }
        }

        /**
         * Returns the current active state of the state machine.
         *
         * This is a snapshot of the current state at the time of access. It may change immediately
         * if the state machine transitions to a new state.
         */
        inline val <T : State<T>> StateMachine<T>.currentState: T
            get() = stateFlow.value
    }

    /**
     * A [StateFlow] that emits the current active state.
     *
     * Consumers can collect this flow to observe the current state at any time. This represents
     * the authoritative, latest state of the machine.
     */
    val stateFlow: StateFlow<T>

    /**
     * Starts executing the state machine.
     *
     * This function launches the state machine loop and begins processing the active state
     * and any subsequent state transitions.
     *
     * The state machine continues running until the associated [CoroutineScope] is cancelled.
     *
     * **Important:** Calling [run] multiple times is not allowed and results in undefined behavior.
     */
    suspend fun run()

    /**
     * Overrides the active state with a new [Transition].
     *
     * The current state will be cancelled and the state machine will immediately transition
     * to [stateTransition.toState].
     *
     * @param stateTransition The transition to apply, including the new state and optional
     *   reason or cause.
     */
    fun overrideState(stateTransition: Transition<T>)
}

internal class StateMachineImpl<T : State<T>> (
    initialState: T,
    private val stateMachineName: String,
    private val stateObserver: StateObserver?,
) : StateMachine<T> {

    private val overrideStateTransition = MutableStateFlow<Transition<T>?>(null)

    private val mutableStateFlow = MutableStateFlow(initialState)
    override val stateFlow: StateFlow<T> = mutableStateFlow.asStateFlow()

    override suspend fun run() = coroutineScope {
        while (coroutineContext.isActive) {
            val fromState = mutableStateFlow.value

            val regularStateTransition = async { fromState.enter() }
            val transitionDueToOverride = async { overrideStateTransition.filterNotNull().first() }

            val transition = select {
                regularStateTransition.onAwait {
                    transitionDueToOverride.cancel()
                    it
                }
                transitionDueToOverride.onAwait {
                    regularStateTransition.cancel()
                    overrideStateTransition.value = null
                    it
                }
            }

            stateObserver?.stateTransition(stateMachineName, fromState.name, transition.toState.name, transition.reason, transition.cause)
            mutableStateFlow.value = transition.toState
        }
    }

    override fun overrideState(stateTransition: Transition<T>) {
        overrideStateTransition.value = stateTransition
    }
}
