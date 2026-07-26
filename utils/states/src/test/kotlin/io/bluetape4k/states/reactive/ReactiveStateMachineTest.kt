package io.bluetape4k.states.reactive

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.states.api.StateMachineException
import io.bluetape4k.states.core.on
import io.bluetape4k.states.core.state
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReactiveStateMachineTest {

    private sealed interface Step
    private data object A: Step
    private data object B: Step
    private data object Done: Step
    private data class Editing(val id: Int, val text: String): Step

    private sealed class Event {
        data object Flip: Event()
        data object Finish: Event()
        data object Change: Event()
        data object Cancel: Event()
    }

    private sealed class Effect {
        data object Flipped: Effect()
    }

    @Test
    fun `send processes concurrent events sequentially`() = runTest {
        val machine = reactiveStateMachine<Step, Event, Effect>(backgroundScope) {
            initialState = A
            transition(A, on<Event.Flip>(), to = B)
            transition(B, on<Event.Flip>(), to = A)
        }

        List(20) {
            async { machine.send(Event.Flip) }
        }.awaitAll()

        machine.currentState shouldBeEqualTo A
        machine.close()
    }

    @Test
    fun `transition effects are delivered as one-time effects`() = runTest {
        val machine = reactiveStateMachine<Step, Event, Effect>(backgroundScope) {
            initialState = A
            transition(A, on<Event.Flip>(), to = B) {
                effect { _, _, _ -> emit(Effect.Flipped) }
            }
        }
        val effects = mutableListOf<Effect>()
        val collector = backgroundScope.launch {
            machine.effects.take(1).toList(effects)
        }
        runCurrent()

        machine.send(Event.Flip)
        collector.join()

        effects shouldBeEqualTo listOf(Effect.Flipped)
        machine.stateFlow.value shouldBeEqualTo B
        machine.close()
    }

    @Test
    fun `state side effect starts on entry and cancels on exit`() = runTest {
        var starts = 0
        var cancellations = 0
        val machine = reactiveStateMachine<Step, Event, Effect>(backgroundScope) {
            initialState = A
            transition(A, on<Event.Flip>(), to = B)
            onState(A) {
                sideEffect {
                    starts++
                    try {
                        awaitCancellation()
                    } finally {
                        cancellations++
                    }
                }
            }
        }
        runCurrent()

        machine.send(Event.Flip)
        runCurrent()

        starts shouldBeEqualTo 1
        cancellations shouldBeEqualTo 1
        machine.close()
    }

    @Test
    fun `keyed side effect does not restart when logical key is unchanged`() = runTest {
        var starts = 0
        val first = Editing(1, "a")
        val second = Editing(1, "b")
        val third = Editing(1, "c")
        val machine = reactiveStateMachine<Step, Event, Effect>(backgroundScope) {
            initialState = first
            transition(first, on<Event.Change>(), to = second)
            transition(second, on<Event.Change>(), to = third)
            onState(state<Editing>()) {
                sideEffect(key = { (it as Editing).id }) {
                    starts++
                    awaitCancellation()
                }
            }
        }
        runCurrent()

        machine.send(Event.Change)
        machine.send(Event.Change)
        runCurrent()

        starts shouldBeEqualTo 1
        machine.currentState shouldBeEqualTo third
        machine.close()
    }

    @Test
    fun `side effect without key restarts when matching state data changes`() = runTest {
        var starts = 0
        val first = Editing(1, "a")
        val second = Editing(1, "b")
        val machine = reactiveStateMachine<Step, Event, Effect>(backgroundScope) {
            initialState = first
            transition(first, on<Event.Change>(), to = second)
            onState(state<Editing>()) {
                sideEffect {
                    starts++
                    awaitCancellation()
                }
            }
        }
        runCurrent()

        machine.send(Event.Change)
        runCurrent()

        starts shouldBeEqualTo 2
        machine.close()
    }

    @Test
    fun `transition effect can enqueue follow-up event without deadlock`() = runTest {
        val machine = reactiveStateMachine<Step, Event, Effect>(backgroundScope) {
            initialState = A
            transition(A, on<Event.Flip>(), to = B) {
                effect { _, _, _ -> send(Event.Finish) }
            }
            transition(B, on<Event.Finish>(), to = Done)
        }

        machine.send(Event.Flip)
        runCurrent()

        machine.currentState shouldBeEqualTo Done
        machine.close()
    }

    @Test
    fun `failed follow-up event does not cancel the machine`() = runTest {
        val machine = reactiveStateMachine<Step, Event, Effect>(backgroundScope) {
            initialState = A
            transition(A, on<Event.Flip>(), to = B) {
                effect { _, _, _ -> send(Event.Finish) }
            }
            transition(B, on<Event.Flip>(), to = A)
        }

        machine.send(Event.Flip)
        runCurrent()
        machine.currentState shouldBeEqualTo B

        machine.send(Event.Flip)

        machine.currentState shouldBeEqualTo A
        machine.close()
    }

    @Test
    fun `cancellation from transition effect is propagated`() = runTest {
        val machine = reactiveStateMachine<Step, Event, Effect>(backgroundScope) {
            initialState = A
            transition(A, on<Event.Flip>(), to = B) {
                effect { _, _, _ -> throw CancellationException("cancelled by test") }
            }
        }

        assertFailsWith<CancellationException> {
            machine.send(Event.Flip)
        }
        machine.close()
    }

    @Test
    fun `transition effect cancellation still restarts state side effects`() = runTest {
        var initialStarts = 0
        var initialCancellations = 0
        var targetStarts = 0
        val machine = reactiveStateMachine<Step, Event, Effect>(backgroundScope) {
            initialState = A
            transition(A, on<Event.Flip>(), to = B) {
                effect { _, _, _ -> throw CancellationException("cancelled by test") }
            }
            onState(A) {
                sideEffect {
                    initialStarts++
                    try {
                        awaitCancellation()
                    } finally {
                        initialCancellations++
                    }
                }
            }
            onState(B) {
                sideEffect {
                    targetStarts++
                    awaitCancellation()
                }
            }
        }
        runCurrent()

        assertFailsWith<CancellationException> {
            machine.send(Event.Flip)
        }
        runCurrent()

        machine.currentState shouldBeEqualTo B
        initialStarts shouldBeEqualTo 1
        initialCancellations shouldBeEqualTo 1
        targetStarts shouldBeEqualTo 1
        machine.close()
    }

    @Test
    fun `close cancels side effects and rejects future events`() = runTest {
        var cancellations = 0
        val machine = reactiveStateMachine<Step, Event, Effect>(backgroundScope) {
            initialState = A
            transition(A, on<Event.Flip>(), to = B)
            onState(A) {
                sideEffect {
                    try {
                        awaitCancellation()
                    } finally {
                        cancellations++
                    }
                }
            }
        }
        runCurrent()

        machine.close()
        runCurrent()

        cancellations shouldBeEqualTo 1
        assertFailsWith<StateMachineException> {
            machine.send(Event.Flip)
        }
    }
}
