package io.github.hylkeb.susstatemachine

/**
 * [StateObserver] is used as an observer class specifically for troubleshooting and debug purposes.
 * By design it only yields state transitions in String format, and not as strongly typed
 * states.
 *
 * If the states must be acted upon when transitioning, it is encouraged to using the typed states
 * emitted from the [StateMachine.stateFlow] method.
 *
 * Example usage:
 *  ```
 *  val stateMachine: StateMachine(
 *      initialState = MyState(),
 *      stateMachineName = "MyStateMachine",
 *      stateObserver = { stateMachine, fromState, toState, reason, cause ->
 *          val transitionString = "[$stateMachine]: ($fromState) => ($toState)"
 *          val reasonString = if (reason != null) " [Reason: $reason]" else ""
 *          val causeString = if (cause != null) " [Cause: ${cause.message}]" else ""
 *          println(transitionString + reasonString + causeString)
 *          // prints for example:
 *          // [MyStateMachine]: (MyState) => (NextState)
 *          // or
 *          // [MyStateMachine]: (MyState) => (ErrorState) [Reason: api failure] [Cause: Could not get a request from the server]
 *      }
 *  )
 *  ```
 */
fun interface StateObserver {

    /**
     * Called whenever the state machine received a new [Transition].
     * Normally this is called when the active [State.enter] returned a new [Transition]
     * It could also be called when a new [Transition] is obtained through the [StateMachine.overrideState] method.
     *
     * > Note: It's wise to specifically mention in the [reason][Transition.reason] of the [Transition]
     * > provided in the [overrideState][StateMachine.overrideState] method that the transition
     * > happened through a state override, otherwise its not visible in the logs that the fromState
     * > was cancelled instead of successfully returned a new state.
     *
     * This [stateTransition] method is called before the toState is [entered][State.enter], meaning
     * that in very rare race conditions, this toState might never become active ([entered][State.enter])
     * This happens when the coroutineContext of the [StateMachine.run] method is cancelled at just the right time.
     *
     * @param stateMachine The name of the state machine in which the transition took place
     * @param fromState The [name][State.name] of the previous active state
     * @param toState The [name][State.name] of the state that is about to become active (as provided by the [transition][Transition.toState])
     * @param reason The [reason][Transition.reason] of the state transition
     * @param cause The [cause][Transition.cause] of the state transition
     */
    fun stateTransition(
        stateMachine: String,
        fromState: String,
        toState: String,
        reason: String?,
        cause: Throwable?,
    )
}
