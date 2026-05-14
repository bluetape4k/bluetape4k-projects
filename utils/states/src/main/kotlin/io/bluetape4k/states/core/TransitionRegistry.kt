package io.bluetape4k.states.core

import io.bluetape4k.states.api.StateMachineException
import java.util.ArrayDeque

internal class TransitionRegistry<S: Any, E: Any>(
    private val exactTransitions: Map<TransitionKey<S, E>, TransitionTarget<S, E>>,
    private val parentTransitions: Map<ParentTransitionKey<S, E>, TransitionTarget<S, E>>,
) {

    fun resolve(state: S, event: E): TransitionMatch<S, E>? {
        val eventType = event::class.java
        val exactKey = TransitionKey(state, eventType)
        exactTransitions[exactKey]?.let { target ->
            return TransitionMatch(target, exactKey, null)
        }

        val parentMatches = parentTransitions
            .mapNotNull { (key, target) ->
                if (key.eventType != eventType) {
                    null
                } else {
                    inheritanceDistance(state::class.java, key.stateType)
                        ?.let { distance -> ParentMatch(key, target, distance) }
                }
            }

        if (parentMatches.isEmpty()) {
            return null
        }

        val nearestDistance = parentMatches.minOf { it.distance }
        val nearest = parentMatches.filter { it.distance == nearestDistance }
        if (nearest.size > 1) {
            throw StateMachineException(
                "Ambiguous inherited transition: state=$state, event=${eventType.simpleName}, " +
                        "parents=${nearest.map { it.key.stateType.simpleName }}"
            )
        }

        return nearest.single().let { match ->
            TransitionMatch(match.target, null, match.key)
        }
    }

    fun allowedEvents(state: S): Set<Class<out E>> {
        val exactEvents = exactTransitions.keys
            .filter { it.state == state }
            .map { it.eventType }

        val parentEvents = parentTransitions.keys
            .filter { it.stateType.isInstance(state) }
            .map { it.eventType }

        return (exactEvents + parentEvents).toSet()
    }

    fun validateKnownStates(knownStates: Set<S>) {
        if (parentTransitions.size < 2 || knownStates.isEmpty()) {
            return
        }

        val parentKeysByEvent = parentTransitions.keys.groupBy { it.eventType }
        parentKeysByEvent.forEach { (eventType, keys) ->
            knownStates.forEach { state ->
                val matches = keys.mapNotNull { key ->
                    inheritanceDistance(state::class.java, key.stateType)
                        ?.let { distance -> key to distance }
                }
                if (matches.size > 1) {
                    val nearestDistance = matches.minOf { it.second }
                    val nearest = matches.filter { it.second == nearestDistance }
                    if (nearest.size > 1) {
                        throw StateMachineException(
                            "Ambiguous inherited transition: state=$state, event=${eventType.simpleName}, " +
                                    "parents=${nearest.map { it.first.stateType.simpleName }}"
                        )
                    }
                }
            }
        }
    }

    private fun inheritanceDistance(actualType: Class<*>, expectedParent: Class<*>): Int? {
        if (!expectedParent.isAssignableFrom(actualType)) {
            return null
        }

        val visited = mutableSetOf<Class<*>>()
        val queue = ArrayDeque<Pair<Class<*>, Int>>()
        queue.add(actualType to 0)

        while (queue.isNotEmpty()) {
            val (type, distance) = queue.removeFirst()
            if (!visited.add(type)) {
                continue
            }
            if (type == expectedParent) {
                return distance
            }

            type.superclass?.let { queue.add(it to distance + 1) }
            type.interfaces.forEach { queue.add(it to distance + 1) }
        }

        return null
    }

    private class ParentMatch<S: Any, E: Any>(
        val key: ParentTransitionKey<S, E>,
        val target: TransitionTarget<S, E>,
        val distance: Int,
    )
}

internal class TransitionMatch<S: Any, E: Any>(
    val target: TransitionTarget<S, E>,
    val exactKey: TransitionKey<S, E>?,
    val parentKey: ParentTransitionKey<S, E>?,
)

