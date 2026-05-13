package io.bluetape4k.states.reactive

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.states.api.StateMachineException
import io.bluetape4k.states.api.TransitionResult
import io.bluetape4k.states.core.ParentTransitionKey
import io.bluetape4k.states.core.TransitionKey
import io.bluetape4k.states.core.TransitionRegistry
import io.bluetape4k.states.core.TransitionTarget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock as withJvmLock

internal class DefaultReactiveStateMachine<S: Any, E: Any, F: Any>(
    override val initialState: S,
    override val finalStates: Set<S>,
    transitions: Map<TransitionKey<S, E>, TransitionTarget<S, E>>,
    parentTransitions: Map<ParentTransitionKey<S, E>, TransitionTarget<S, E>>,
    private val transitionEffects: Map<TransitionKey<S, E>, suspend ReactiveEffectScope<E, F>.(S, E, S) -> Unit>,
    private val parentTransitionEffects: Map<ParentTransitionKey<S, E>, suspend ReactiveEffectScope<E, F>.(S, E, S) -> Unit>,
    private val sideEffects: List<ReactiveSideEffect<S, E, F>>,
    parentScope: CoroutineScope,
): ReactiveStateMachine<S, E, F> {

    companion object: KLogging()

    private val machineJob = SupervisorJob(parentScope.coroutineContext[Job])
    private val machineScope = CoroutineScope(parentScope.coroutineContext + machineJob)
    private val mutex = Mutex()
    private val registry = TransitionRegistry(transitions, parentTransitions)
    private val sideEffectsLock = ReentrantLock()
    private val activeSideEffects = mutableMapOf<Int, ActiveSideEffect>()
    private val _stateFlow = MutableStateFlow(initialState)
    private val _effects = MutableSharedFlow<F>(replay = 0, extraBufferCapacity = 64)
    @Volatile
    private var closed = false

    override val stateFlow: StateFlow<S> = _stateFlow.asStateFlow()
    override val effects = _effects.asSharedFlow()
    override val value: S
        get() = _stateFlow.value
    override val replayCache: List<S>
        get() = stateFlow.replayCache
    override val currentState: S
        get() = _stateFlow.value

    init {
        restartSideEffects(initialState)
    }

    override suspend fun collect(collector: FlowCollector<S>): Nothing {
        stateFlow.collect(collector)
    }

    override suspend fun send(event: E): TransitionResult<S, E> = mutex.withLock {
        if (closed) {
            throw StateMachineException("State machine is closed.")
        }

        val previous = _stateFlow.value

        if (previous in finalStates) {
            throw StateMachineException("이미 종료 상태입니다: $previous")
        }

        val match = registry.resolve(previous, event)
            ?: throw StateMachineException(
                "허용되지 않은 전이: $previous + ${event::class.simpleName}. " +
                        "허용된 이벤트: ${allowedEvents().map { it.simpleName }}"
            )
        val target = match.target
        val guardResult = target.guard?.invoke(previous, event) ?: true
        if (!guardResult) {
            throw StateMachineException("Guard 조건 실패: $previous + ${event::class.simpleName}")
        }

        _stateFlow.value = target.state
        log.debug { "$previous --[${event::class.simpleName}]--> ${target.state}" }

        try {
            val effectScope = ReactiveEffectScope<E, F>(
                emitEffect = { effect -> _effects.emit(effect) },
                sendEvent = { nextEvent -> launchFollowUpEvent(nextEvent) },
            )
            match.exactKey?.let { key ->
                transitionEffects[key]?.invoke(effectScope, previous, event, target.state)
            }
            match.parentKey?.let { key ->
                parentTransitionEffects[key]?.invoke(effectScope, previous, event, target.state)
            }
        } finally {
            restartSideEffects(target.state)
        }

        TransitionResult(
            previousState = previous,
            event = event,
            currentState = target.state,
        )
    }

    override fun canTransition(event: E): Boolean {
        val state = currentState
        if (state in finalStates) return false

        val target = registry.resolve(state, event)?.target ?: return false
        return target.guard?.invoke(state, event) ?: true
    }

    override fun allowedEvents(): Set<Class<out E>> {
        val state = currentState
        if (state in finalStates) return emptySet()

        return registry.allowedEvents(state)
    }

    override fun close() {
        sideEffectsLock.withJvmLock {
            if (closed) {
                return
            }
            closed = true
            activeSideEffects.values.forEach { it.job.cancel() }
            activeSideEffects.clear()
        }
        machineScope.cancel()
    }

    private fun restartSideEffects(state: S) {
        sideEffectsLock.withJvmLock {
            if (closed) {
                return
            }

            val desired = sideEffects
                .mapIndexedNotNull { index, sideEffect ->
                    if (sideEffect.matches(state)) {
                        index to sideEffect.key(state)
                    } else {
                        null
                    }
                }
                .toMap()

            activeSideEffects
                .filter { (index, active) -> desired[index] != active.key }
                .keys
                .toList()
                .forEach { index ->
                    activeSideEffects.remove(index)?.job?.cancel()
                }

            desired.forEach { (index, key) ->
                if (activeSideEffects[index]?.key == key) {
                    return@forEach
                }

                val sideEffect = sideEffects[index]
                val effectScope = ReactiveEffectScope<E, F>(
                    emitEffect = { effect -> _effects.emit(effect) },
                    sendEvent = { event -> launchFollowUpEvent(event) },
                )
                val job = machineScope.launch {
                    sideEffect.block.invoke(effectScope, state)
                }
                activeSideEffects[index] = ActiveSideEffect(key, job)
            }
        }
    }

    private class ActiveSideEffect(
        val key: Any?,
        val job: Job,
    )

    private fun launchFollowUpEvent(event: E) {
        if (closed) {
            return
        }
        machineScope.launch {
            if (closed) {
                return@launch
            }
            try {
                send(event)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.warn(e) { "Follow-up event failed: ${event::class.simpleName}" }
            }
        }
    }
}
