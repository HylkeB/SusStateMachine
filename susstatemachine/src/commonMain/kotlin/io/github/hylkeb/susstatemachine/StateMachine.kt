package io.github.hylkeb.susstatemachine

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

/**
 * The StateMachine class operates on the concrete set of states of type T.
 * When the StateMachine runs, it performs the following actions in a loop for as long as the coroutineContext is active:
 * 1. Call the [State.enter] method of the active [State];
 * 2. Wait until a new [Transition] is returned. It will inform the [StateObserver] that a state transition is about to take place;
 * 3. Update the active state to the one returned from [Transition.toState];
 * 4. Back to step 1.
 *
 * Additionally, the active state of the StateMachine can be overridden by the method [StateMachine.overrideState].
 * When this is called, the active state will be cancelled and instead the transition provided by the override
 * will be used in step 2.
 *
 * This interface should be instantiated as [StateMachineImpl], or it can be instantiated using a mocking
 * library for testing purposes.
 */
interface StateMachine<T : State<T>> {

    companion object {
        /**
         * Creates a new [StateMachine].
         *
         * @param initialState The initial state that will be entered once the StateMachine is [run].
         * @param stateMachineName The name of this StateMachine used in the [stateObserver].
         * @param stateObserver A [StateObserver] that gets called every time a state transition is about to happen.
         */
        operator fun <T : State<T>> invoke(initialState: T, stateMachineName: String, stateObserver: StateObserver?): StateMachine<T> {
            @Suppress("DEPRECATION")
            return StateMachineImpl(initialState, stateMachineName, stateObserver)
        }
    }

    /**
     * This flow emits the active states with it's consumers.
     * It has a replay value of 1 so the stateFlow always directly emits the active state for new
     * consumers. It is initially initialised with the provided initialState.
     */
    val stateFlow: SharedFlow<T>

    /**
     * It is required to call this method to actually run this state machine.
     * Because the state machine calls the suspending [State.enter] methods, it requires a coroutineContext
     * to do so.
     *
     * When cancelling this run method, the [State.enter] method of the active state is also cancelled.
     * It will also stop entering any new states.
     */
    suspend fun run()

    /**
     * Overrides the active state. It will result in the active state being cancelled and then it will
     * transition to the state provided in this [stateTransition].
     * @param stateTransition The state to transition to.
     */
    fun overrideState(stateTransition: Transition<T>)
}

/**
 * The StateMachine class operates on the concrete set of states of type T.
 * When the StateMachine runs, it performs the following actions in a loop for as long as the coroutineContext is active:
 * 1. Call the [State.enter] method of the active [State];
 * 2. Wait until a new [Transition] is returned. It will inform the [StateObserver] that a state transition is about to take place;
 * 3. Update the active state to the one returned from [Transition.toState];
 * 4. Back to step 1.
 *
 * Additionally, the active state of the StateMachine can be overridden by the method [StateMachine.overrideState].
 * When this is called, the active state will be cancelled and instead the transition provided by the override
 * will be used in step 2.
 *
 */
@Deprecated("Use invoke method on StateMachine", replaceWith = ReplaceWith("StateMachine<T>"))
class StateMachineImpl<T : State<T>>
@Deprecated("", ReplaceWith("StateMachine(initialState, stateMachineName, stateObserver)", "io.github.hylkeb.susstatemachine.StateMachine")) constructor(
    initialState: T,
    private val stateMachineName: String = "state-machine",
    private val stateObserver: StateObserver? = null,
) : StateMachine<T> {

    private val myself: StateMachine<T> = this
    private val mutableSharedFlow = MutableSharedFlow<T>(1).apply { tryEmit(initialState) }
    private val overrideStateTransition = MutableStateFlow<Transition<T>?>(null)

    override val stateFlow: SharedFlow<T> = mutableSharedFlow.asSharedFlow()


    override suspend fun run(): Unit = coroutineScope {
        while (coroutineContext.isActive) {
            val fromState = stateFlow.first()
            fromState.becameActive(myself)

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
            mutableSharedFlow.emit(transition.toState)
        }
    }

    override fun overrideState(stateTransition: Transition<T>) {
        overrideStateTransition.value = stateTransition
    }
}
