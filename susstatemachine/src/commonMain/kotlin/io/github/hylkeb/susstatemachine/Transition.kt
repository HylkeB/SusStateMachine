package io.github.hylkeb.susstatemachine

/**
 * This class describes the transition to a new [State].
 * This class should be instantiated from within the [State.enter] method to indicate what the new
 * state should become once the active [State] has completed its work.
 *
 * This class could also be instantiated by the managing class of the [StateMachine], when for some
 * reason the active state must be overridden via the [StateMachine.overrideState] method.
 *
 * The [toState] is used as the new state that must become active.
 * The [reason] and [cause] are provided to the [StateObserver] and function purely as values helpful
 * during troubleshooting.
 */
class Transition<T : State<T>>(
    /**
     * The [State] to transition to.
     */
    val toState: T,

    /**
     * Optionally the reason why this transition took place.
     */
    val reason: String? = null,

    /**
     * Optionally the cause with stacktrace of why this transition took place.
     */
    val cause: Throwable? = null,
)
