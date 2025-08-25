package io.github.hylkeb.susstatemachine

/**
 * This class should be implemented by a concrete set of states on which a [StateMachine] operates.
 * Ideally that should be performed by defining a sealed class of the StateType, which every concrete state implements.
 */
interface State<out T : State<T>> {

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
}
