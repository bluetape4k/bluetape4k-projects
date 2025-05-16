package io.bluetape4k.examples.coroutines.flow

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test
import kotlin.coroutines.coroutineContext

class SharedFlowAsEventBus {

    companion object: KLoggingChannel()

    /**
     * An event bus implementation that uses a shared flow to broadcast events to multiple listeners.
     */
    class EventBus<T> {
        // The shared flow that will be used to broadcast events to multiple listeners.
        private val _events = MutableSharedFlow<T>(replay = 0, extraBufferCapacity = 64)

        // The public flow that will be used to listen to events.
        val events: Flow<T> = _events.asSharedFlow()

        suspend fun sendEvent(event: T) {
            _events.emit(event)
        }

        suspend fun close() {
            coroutineContext.cancelChildren()
        }
    }

    sealed class Event {
        data object EventA: Event()
        data object EventB: Event()
        data class EventC(val value: Int): Event()
    }

    class EventListener(
        private val name: String,
        private val eventBus: EventBus<Event>,
        private val scope: CoroutineScope,
    ) {

        companion object: KLogging()

        init {
            // Subscribe to the events flow using the onEach operator
            eventBus.events
                .onEach { event ->
                    when (event) {
                        is Event.EventA -> handleEventA(event)
                        is Event.EventB -> handleEventB(event)
                        is Event.EventC -> handleEventC(event)
                    }
                }
                // Launch the event listener in the given coroutine scope
                // It can cancel the subscription when scope is not present any more
                // `scope.coroutineContext.cancelChildren()` 을 호출하면 중단됩니다.
                .launchIn(scope)
        }

        private fun handleEventA(event: Event.EventA) {
            log.debug { "$name: EventA received. event=$event" }
        }

        private fun handleEventB(event: Event.EventB) {
            log.debug { "$name: EventB received. event=$event" }

        }

        private fun handleEventC(event: Event.EventC) {
            log.debug { "$name: EventC received. event=$event" }
        }
    }

    @Suppress("UNUSED_VARIABLE")
    @Test
    fun `event bus example`() = runSuspendIO {

        val eventBus = EventBus<Event>()

        // Create event listeners
        val listener1 = EventListener("#1", eventBus, this)
        val listener2 = EventListener("#2", eventBus, this)

        val job = launch(Dispatchers.Default) {
            // Send events
            delay(100)
            eventBus.sendEvent(Event.EventA)
            delay(100)
            eventBus.sendEvent(Event.EventB)
            delay(100)
            eventBus.sendEvent(Event.EventC(42))
        }

        job.join()
        // Wait for the listeners to process the events
        delay(500)

        eventBus.close()
    }
}
