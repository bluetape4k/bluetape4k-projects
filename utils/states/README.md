# bluetape4k-states

English | [한국어](./README.ko.md)

A Kotlin DSL-based finite state machine (FSM) library. It supports both synchronous and coroutine-based FSMs, along with guard conditions and
`StateFlow`-based state observation.

## Architecture

### Concept Overview

How states, events, and the state machine interact:

```mermaid
flowchart LR
    subgraph DSL["DSL Definition"]
        S["States\n(enum / sealed)"]
        E["Events\n(sealed class)"]
        T["Transitions\nfrom → on<Event> → to"]
        G["Guard Conditions\n(optional predicate)"]
    end

    User -->|" stateMachine { ... } "| DSL
    DSL -->|" build() "| SM[StateMachine]
    SM -->|" transition(event) "| TR["TransitionResult\n(prev, event, current)"]
    SM -.->|" stateFlow (suspend) "| SF["StateFlow\n(observable state)"]
    TR -->|" onTransition callback "| CB["Side-effect Logic"]
```

A `StateMachine` holds a set of typed **transitions** (from-state + event-type → to-state).  
Each transition can have an optional **guard condition** that must pass before the state changes.  
`SuspendStateMachine` adds a `StateFlow` so consumers can reactively observe state changes.

### Class Diagram

```mermaid
classDiagram
    class BaseStateMachine~S, E~ {
        <<interface>>
        +currentState: S
        +initialState: S
        +finalStates: Set~S~
        +canTransition(event: E): Boolean
        +allowedEvents(): Set~Class~E~~
        +isInFinalState(): Boolean
    }

    class StateMachine~S, E~ {
        <<interface>>
        +transition(event: E): TransitionResult~S, E~
    }

    class SuspendStateMachineInterface~S, E~ {
        <<interface>>
        +stateFlow: StateFlow~S~
        +transition(event: E): TransitionResult~S, E~
    }

    class DefaultStateMachine~S, E~ {
        -_currentState: AtomicReference~S~
        -transitions: Map~TransitionKey, TransitionTarget~
        -onTransition: ((S, E, S) -> Unit)?
        +transition(event: E): TransitionResult~S, E~
        +canTransition(event: E): Boolean
        +allowedEvents(): Set~Class~E~~
    }

    class SuspendStateMachine~S, E~ {
        -mutex: Mutex
        -_stateFlow: MutableStateFlow~S~
        -transitions: Map~TransitionKey, TransitionTarget~
        -onTransition: (suspend (S, E, S) -> Unit)?
        +transition(event: E): TransitionResult~S, E~
        +canTransition(event: E): Boolean
        +allowedEvents(): Set~Class~E~~
    }

    class TransitionResult~S, E~ {
        +previousState: S
        +event: E
        +currentState: S
    }

    class TransitionKey~S, E~ {
        +state: S
        +eventType: Class~E~
    }

    class TransitionTarget~S, E~ {
        +state: S
        +guard: ((S, E) -> Boolean)?
    }

    class StateMachineException {
        +message: String
    }

    BaseStateMachine <|-- StateMachine
    BaseStateMachine <|-- SuspendStateMachineInterface
    StateMachine <|.. DefaultStateMachine
    SuspendStateMachineInterface <|.. SuspendStateMachine
    DefaultStateMachine ..> TransitionKey : uses
    DefaultStateMachine ..> TransitionTarget : uses
    SuspendStateMachine ..> TransitionKey : uses
    SuspendStateMachine ..> TransitionTarget : uses
    DefaultStateMachine ..> TransitionResult : returns
    SuspendStateMachine ..> TransitionResult : returns
    DefaultStateMachine ..> StateMachineException : throws
    SuspendStateMachine ..> StateMachineException : throws
```

> `StateMachine` and `SuspendStateMachineInterface` are independent from each other. To avoid a signature clash between
`suspend fun transition()` and `fun transition()`, only read-only properties are shared through the common
`BaseStateMachine`.

### DSL Builder Structure

```mermaid
classDiagram
    class StateMachineBuilder~S, E~ {
        +initialState: S?
        +finalStates: Set~S~
        +transition(from, eventType, to)
        +transition(from, eventType, to, setup)
        +onTransition(handler)
        +build(): DefaultStateMachine~S, E~
    }

    class SuspendStateMachineBuilder~S, E~ {
        +initialState: S?
        +finalStates: Set~S~
        +transition(from, eventType, to)
        +transition(from, eventType, to, setup)
        +onTransition(handler)
        +build(): SuspendStateMachine~S, E~
    }

    class TransitionBuilder~S, E~ {
        +guard: ((S, E) -> Boolean)?
        +guard(predicate)
    }

    StateMachineBuilder ..> TransitionBuilder : creates
    SuspendStateMachineBuilder ..> TransitionBuilder : creates
    StateMachineBuilder ..> DefaultStateMachine : builds
    SuspendStateMachineBuilder ..> SuspendStateMachine : builds
```

## Key Features

- **Type-safe DSL**: concise FSM definitions with `stateMachine {}` and `suspendStateMachine {}`
- **Synchronous FSM**: thread-safe state transitions based on `AtomicReference` CAS
- **Coroutine FSM**: suspend transitions and state observation based on `Mutex` + `StateFlow`
- **Guard conditions**: validate conditions before transitions
- **Final state consistency**: once a final state is reached, `canTransition()` returns `false` and `allowedEvents()` returns an empty set
- **clinic-appointment pattern**: adopts a map-based transition model plus suspend callback pattern

## Example State Diagrams

### 1. Turnstile — Simple FSM

```mermaid
stateDiagram-v2
    [*] --> Locked : initial state

    Locked --> Unlocked : Coin
    Unlocked --> Locked : Push
    Locked --> Locked : Push while locked
    Unlocked --> Unlocked : Coin while already unlocked
```

### 2. Order — One-Way FSM

```mermaid
stateDiagram-v2
    [*] --> CREATED : order created

    CREATED --> PAID : Pay
    CREATED --> CANCELLED : Cancel
    PAID --> SHIPPED : Ship
    SHIPPED --> DELIVERED : Deliver

    DELIVERED --> [*]
    CANCELLED --> [*]
```

### 3. Appointment — Complex FSM (`clinic-appointment`)

```mermaid
stateDiagram-v2
    direction LR
    [*] --> PENDING : appointment created

    PENDING --> REQUESTED : Request
    PENDING --> CANCELLED : Cancel

    REQUESTED --> CONFIRMED : Confirm
    REQUESTED --> PENDING_RESCHEDULE : RequestReschedule
    REQUESTED --> CANCELLED : Cancel

    CONFIRMED --> CHECKED_IN : CheckIn
    CONFIRMED --> NO_SHOW : MarkNoShow
    CONFIRMED --> PENDING : Reschedule
    CONFIRMED --> PENDING_RESCHEDULE : RequestReschedule
    CONFIRMED --> CANCELLED : Cancel

    PENDING_RESCHEDULE --> RESCHEDULED : ConfirmReschedule
    PENDING_RESCHEDULE --> CANCELLED : Cancel

    CHECKED_IN --> IN_PROGRESS : StartTreatment
    CHECKED_IN --> CANCELLED : Cancel

    IN_PROGRESS --> COMPLETED : Complete

    COMPLETED --> [*]
    NO_SHOW --> [*]
    CANCELLED --> [*]
    RESCHEDULED --> [*]
```

## Quick Start

### Dependency

```kotlin
dependencies {
    implementation(project(":bluetape4k-states"))
}
```

### Synchronous FSM

```kotlin
val orderFsm = stateMachine<OrderState, OrderEvent> {
    initialState = OrderState.CREATED
    finalStates = setOf(OrderState.DELIVERED, OrderState.CANCELLED)

    transition(OrderState.CREATED, on<OrderEvent.Pay>(), to = OrderState.PAID)
    transition(OrderState.PAID, on<OrderEvent.Ship>(), to = OrderState.SHIPPED)
    transition(OrderState.SHIPPED, on<OrderEvent.Deliver>(), to = OrderState.DELIVERED)
    transition(OrderState.CREATED, on<OrderEvent.Cancel>(), to = OrderState.CANCELLED)

    onTransition { prev, event, next ->
        println("$prev --[$event]--> $next")
    }
}

val result = orderFsm.transition(OrderEvent.Pay())
// result.previousState == CREATED
// result.currentState == PAID
```

### Coroutine FSM

```kotlin
val suspendFsm = suspendStateMachine<AppointmentState, AppointmentEvent> {
    initialState = AppointmentState.PENDING
    finalStates = setOf(AppointmentState.COMPLETED, AppointmentState.CANCELLED)

    transition(AppointmentState.PENDING, on<AppointmentEvent.Request>(), to = AppointmentState.REQUESTED)
    transition(AppointmentState.REQUESTED, on<AppointmentEvent.Confirm>(), to = AppointmentState.CONFIRMED)

    onTransition { prev, event, next ->
        println("State transition: $prev --> $next")
    }
}

// observe StateFlow
launch { suspendFsm.stateFlow.collect { state -> println("Current state: $state") } }

// suspend transition
val result = suspendFsm.transition(AppointmentEvent.Request())
```

### Guard Conditions

```kotlin
val fsm = stateMachine<State, Event> {
    initialState = State.PENDING

    transition(State.PENDING, on<ApproveEvent>(), to = State.APPROVED) {
        guard { state, event -> (event as ApproveEvent).approvedBy != null }
    }
}
```

## State Transition Sequence Diagrams

### Synchronous FSM Transition Flow

```mermaid
sequenceDiagram
    participant Caller
    participant DefaultStateMachine
    participant AtomicReference
    participant TransitionMap
    participant OnTransitionCallback

    Caller->>DefaultStateMachine: transition(event)
    DefaultStateMachine->>AtomicReference: get() → previousState
    DefaultStateMachine->>DefaultStateMachine: finalStates.contains(previousState)?
    alt final state
        DefaultStateMachine-->>Caller: throw StateMachineException
    end
    DefaultStateMachine->>TransitionMap: get(TransitionKey(previousState, event::class))
    alt no transition
        DefaultStateMachine-->>Caller: throw StateMachineException
    end
    DefaultStateMachine->>DefaultStateMachine: guard?.invoke(previousState, event)?
    alt guard failed
        DefaultStateMachine-->>Caller: throw StateMachineException
    end
    DefaultStateMachine->>AtomicReference: compareAndSet(previousState, nextState)
    alt CAS failure (concurrent transition conflict)
        DefaultStateMachine-->>Caller: throw StateMachineException
    end
    DefaultStateMachine->>OnTransitionCallback: invoke(previous, event, next)
    DefaultStateMachine-->>Caller: TransitionResult(previous, event, next)
```

### Coroutine FSM Transition Flow (`SuspendStateMachine`)

```mermaid
sequenceDiagram
    participant Caller
    participant SuspendStateMachine
    participant Mutex
    participant MutableStateFlow
    participant OnTransitionCallback

    Caller->>SuspendStateMachine: transition(event) [suspend]
    SuspendStateMachine->>Mutex: withLock { ... }
    Note over Mutex: concurrent transitions are serialized

    Mutex->>MutableStateFlow: value → previousState
    SuspendStateMachine->>SuspendStateMachine: validate finalStates / transitions
    SuspendStateMachine->>SuspendStateMachine: check guard condition
    SuspendStateMachine->>MutableStateFlow: value = nextState
    Note over MutableStateFlow: automatically emitted to StateFlow subscribers
    SuspendStateMachine->>OnTransitionCallback: invoke(previous, event, next)
    SuspendStateMachine-->>Caller: TransitionResult(previous, event, next)
```

## `clinic-appointment` Migration Guide

An existing `AppointmentStateMachine` implemented directly with maps can be replaced with the `suspendStateMachine` DSL:

**Before** (direct implementation):

```kotlin
class AppointmentStateMachine {
    private val transitions: Map<Pair<State, Class<out Event>>, State> = buildMap { ... }
    suspend fun transition(currentState: State, event: Event): State { ... }
}
```

**After** (bluetape4k-states DSL):

```kotlin
val fsm = suspendStateMachine<AppointmentState, AppointmentEvent> {
    initialState = AppointmentState.PENDING
    finalStates = setOf(AppointmentState.COMPLETED, AppointmentState.CANCELLED)

    transition(AppointmentState.PENDING, on<AppointmentEvent.Request>(), to = AppointmentState.REQUESTED)
    transition(AppointmentState.REQUESTED, on<AppointmentEvent.Confirm>(), to = AppointmentState.CONFIRMED)
    // ... register the remaining transitions
}

// usage
val result = fsm.transition(AppointmentEvent.Request())
println(result.currentState) // REQUESTED

// observe StateFlow (new feature)
launch { fsm.stateFlow.collect { state -> updateUI(state) } }
```

**Improvements**:

- declarative definition of states and transitions through DSL
- built-in state observation through `StateFlow`
- support for guard conditions
- transition-history tracking through `TransitionResult`
- concurrency safety guaranteed by `Mutex`
