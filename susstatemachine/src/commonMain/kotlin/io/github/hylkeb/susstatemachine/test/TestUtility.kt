package io.github.hylkeb.susstatemachine.test

import io.github.hylkeb.susstatemachine.State

/**
 * Configures this state in such a way that the [State.awaitTransition] method immediately returns.
 */
fun <T : State<*>> T.prepareForTest(): T {
    becameActive(null)
    return this
}