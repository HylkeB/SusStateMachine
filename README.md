# SusStateMachine
_suspending state machine_

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.hylkeb/susstatemachine)

SusStateMachine is a tiny library to create simple yet powerful and robust finite state machines.
It is short for _**suspending state machine**_

The robustness lies in the fact that each must be implemented as a self-contained class, and can
only execute suspending code when the state is active. The way the StateMachine is implemented it
is impossible to have multiple active states. This eliminates all sorts of concurrency issues.

The way you have to set up the states of your state machine also helps to keep them readable.

## Example

Consider the following simple finite state machine that describes the states of an API request.
This request will be exposed by one class, and can be re-used by multiple consumers.
The state machine makes sure only one actual call is being performed, and it makes it easy for concurrent consumers to reuse the state.

[![](https://mermaid.ink/img/pako:eNp9klFLwzAQx79KiJSAbDDcWx580E0Q3MsUX4wPt-bShbXJuKbiGPvupsloi4gUmsv_988l3N2Zl14jl7wozsoxZp0NkqWQMRH22KCQTOygRTGbqu9AFnY1tmKwR6SBDpuYUDDJAnU4G0jA7_Doa099upvlcilGVluHIwOACTuSbYBOb_8cv1pGvFgsJngH5aEi3zktpLgxxkxYA9Y9HKp8rzYig0u_xN-lKJRrAwRcWagImvnXXXZoS1gG6x172Wbl4_aTzef37FnXmJU-StIThnJvXRVrksKMB7W3rIk8RZ5WpZy5wj-sKwgwZIrP68oS29Z0dfbmTL-u3WKgU-bpeI_jg5XrPz7jDVKshI5jkFqpeGqx4jKGGg10dVA81iNaoQv-9eRKLlN_eXfUY324NFC3g7rWNngaREzbTZ63NHaXH5nNwyg?type=png)](https://mermaid-js.github.io/mermaid-live-editor/edit#pako:eNp9klFLwzAQx79KiJSAbDDcWx580E0Q3MsUX4wPt-bShbXJuKbiGPvupsloi4gUmsv_988l3N2Zl14jl7wozsoxZp0NkqWQMRH22KCQTOygRTGbqu9AFnY1tmKwR6SBDpuYUDDJAnU4G0jA7_Doa099upvlcilGVluHIwOACTuSbYBOb_8cv1pGvFgsJngH5aEi3zktpLgxxkxYA9Y9HKp8rzYig0u_xN-lKJRrAwRcWagImvnXXXZoS1gG6x172Wbl4_aTzef37FnXmJU-StIThnJvXRVrksKMB7W3rIk8RZ5WpZy5wj-sKwgwZIrP68oS29Z0dfbmTL-u3WKgU-bpeI_jg5XrPz7jDVKshI5jkFqpeGqx4jKGGg10dVA81iNaoQv-9eRKLlN_eXfUY324NFC3g7rWNngaREzbTZ63NHaXH5nNwyg)

> Note: This example is also implemented in the unit test of this library, see [SystemTest](./susstatemachine/src/commonTest/kotlin/io/github/hylkeb/susstatemachine/SystemTest.kt).

### Define the states

In this example, the states will be defined using a sealed interface, so each state is easily mock-able by libraries such as [MocKMP](https://github.com/kosi-libs/MocKMP).
Other mocking libraries also allow mocking of concrete classes, in which case a sealed class could also be used instead.

However, by defining the states as a sealed interface, you can easily define the total functionality of the state machine in one file.

```kotlin
sealed interface RequestState : State<RequestState> {
    /** The Idle state of this Request, waits until a fetch is requested before continuing to Fetching */
    interface Idle : RequestState {
        /** Initiate the fetch, results in transition towards Fetching */
        suspend fun fetch()
    }
    
    /** In this state the API will be called and based on the result it will either go to Error or Data */
    interface Fetching : RequestState
    
    /** This state is active is the last fetch failed */
    interface Error : RequestState {
        /** The reason why the fetch failed */
        val cause: Throwable
        /** Retry the fetch, results in a transition towards Fetching */
        suspend fun retry() 
    }
    
    /** The request has successfully been executed and data is available */
    interface Data : RequestState {
        /** The response data */
        val data: String
    }
}
```

### Implementing the states

Each state is defined as one distinct interface, thus it should be implemented as one class per state.
The following code shows some very simple implementation for each of the states.

As each state also implements the `State<T : State>` interface, they must implement the `suspend fun enter(): Transition<T>` method.
This is where the magic happens, this function performs the work a state must do and then returns a transition towards a new state.
Sometimes the work a state must do is just wait for some external signal, and sometimes it can directly start its work as soon as its entered.

The `Transition` class describes a transition to a new toState.
It can optionally contain a reason an cause for troubleshooting.

> Note that some states also extend the `StateImpl` abstract class.
> This class contains a convenience method `awaitTransition()`, which suspends until the state machine has a new active state.
> This is helpful when the transition of this state depends on an external signal.
> See it in action at [using the state machine](#using-the-state-machine).

```kotlin
class IdleStateImpl : StateImpl<RequestState>(), RequestState.Idle {
    private val fetchRequested: CompletableJob = Job()

    override suspend fun enter(): Transition<RequestState> {
        fetchRequested.join()
        return Transition(FetchingStateImpl())
    }

    override suspend fun fetch() {
        fetchRequested.complete()
        awaitTransition() // method defined in StateImpl class
    }
}

class FetchingStateImpl : RequestState.Fetching {
    override suspend fun enter(): Transition<RequestState> {
        // Get api from somewhere, assume its some global property (but its better to provide some DI container for testability)
        val response = api.doApiCall()
            .getOrElse { ex -> 
                return Transition(ErrorStateImpl(ex), "api error", ex)
            }
        return Transition(DataStateImpl(response))
    }
}

class ErrorStateImpl(
    override val cause: Throwable
) : StateImpl<RequestState>(), RequestState.Error {
    private val retryRequested: CompletableJob = Job()
    
    override suspend fun enter(): Transition<RequestState> {
        retryRequested.join()
        return Transition(FetchingStateImpl())
    }
    
    override suspend fun retry() {
        retryRequested.complete()
        awaitTransition()
    }
}

class DataStateImpl(
    override val data: String
) : RequestState.Data {
    override suspend fun enter(): Transition<RequestState> {
        // In this example this is the end state (no refresh possibility)
        // so it just waits until cancellation (which happens if the state machine is cancelled)
        awaitCancellation()
    }
}
```

### Using the state machine

Now that the state machine has been described and implemented, it's time to use it.
In this particular example we're defining a `RequestManager` class which is responsible for
providing the data associated with this request, or an error if it fails.
If multiple consumers request the data simultaneously, it should let every additional consumer piggyback on the already running request.

For this to happen, the `RequestManager` will instantiate a `StateMachine` and run it.

Furthermore it will have one method called `suspend fun getData(): Result<String>`

> Note that the request itself is no longer bound to a particular consumer, but to some other lifecycle.
> This could be any lifecycle, for example just the lifecycle of the application altogether,
> or an arbitrary lifecycle such as the lifecycle of a logged in customer.
> Just make sure that the coroutine that runs the state machine is cancelled at the end.

```kotlin
class RequestManager {
    // Create the state machine with the initial state
    val stateMachine: StateMachine<RequestState> = StateMachineImpl(IdleStateImpl())

    // Let some external lifecycle manager create the coroutine necessary to run this requestManager
    suspend fun run() {
        stateMachine.run()
    }

    // Returns the result of the request, either success or error
    suspend fun getResponse(): Result<String> {
        // First check what the current state is, maybe directly return, or trigger a state transition
        when (val currentState = stateMachine.stateFlow.first()) {
            is RequestState.Data -> return Result.success(currentState.data) // reuse successful response directly
            is RequestState.Idle -> currentState.fetch() // first attempt at getting the response, start fetching
            is RequestState.Fetching -> { /*No direct action needed, will observe states instead*/ }
            is RequestState.Error -> currentState.retry() // new attempt at getting the response, retry the request
        }
        
        // If the fetch() and retry() methods didn't call awaitTransition(), the state wouldn't have updated yet
        // and as a result the statement below would directly return a failure, instead of waiting for the result of the new Fetching state.
        
        // Data wasn't directly available, start observing the flow of states until a terminal state is emitted.
        return stateMachine.stateFlow
            .mapNotNull { state ->
                when (state) {
                    is RequestState.Data -> Result.success(state.data) // fetch resulted in data
                    is RequestState.Fetching -> null // still fetching, wait until next state transition
                    is RequestState.Error -> Result.failure(state.cause) // fetch resulted in an error
                    is RequestState.Idle -> Result.failure(IllegalStateException("RequestState should not be Initial here, was the state overridden?"))
                }
            }
            .first()
    }
}
```

This single manager experiences the benefits of the state machine.

Because the states are defined as a sealed interface or class, it can perform exhaustive when statements,
always rigorously handling every case.

Furthermore because the StateMachine can only be in one state at the same time, and the magic happens in the enter method,
it also benefits from thread-safety. No matter how many concurrent `getResponse()` calls are being made, and potentially concurrent `fetch()` or `retry()`
calls are being made, it can always only result in one state transition, thus one active fetching state.

## Testability

**State**

Each state is its own distinct class, with a very simple interface: an `enter()` method and possibly some external methods.
Even if a state is responsible for doing a lot of complex steps (e.g. fetching data, reading storage, writing storage),
with the proper DI infrastructure it is easy to test all of its transitions.

**StateMachine consumers**

The StateMachine consumer (in the example above the RequestManager) can easily be tested by mocking the
StateMachine, as its a simple interface. Just stub the stateFlow with your own prefilled stateFlow with
mocked states, and you can easily test if your StateMachine consumer works properly, without mocking every
dependency of the states themselves.

## Observability

The `StateMachineImpl` class optionally accepts a `stateMachineName` and a `StateObserver` and the `State` interface defines the property `State.name`.
Whenever a state transition is about to happen, the `StateObserver.stateTransition(..)` method is called.

The StateObserver is defined as following:

```kotlin
interface StateObserver {
    fun stateTransition(
        stateMachine: String, // name as provided when instantiating StateMachineImpl; defaults to "state-machine"
        fromState: String, // name of the state that just completed, defaults to State::class.simpleName.toString()
        toState: String, // name of the state that is about to be entered
        reason: String?, // the reason of the state transition, as defined in the Transition class
        cause: Throwable? // the cause of the state transition, as defined in the Transition class
    )
}
```

On purpose the state observer operates only on strings (and a Throwable), to discourage control flow based
on these events. This observer should only be used for troubleshooting.