package io.bluetape4k.collections.graph

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.toList

object Graph {

    enum class TraversalOrder { DFS, BFS }

    fun <T: Comparable<T>> search(
        order: TraversalOrder,
        source: T,
        adjacents: (T) -> Iterable<T>,
    ): List<T> {
        return traverseGraph(order, source) { adjacents(it).asSequence() }.toList()
    }

    fun <T: Comparable<T>> searchAsSequence(
        order: TraversalOrder,
        source: T,
        adjacents: (T) -> Sequence<T>,
    ): Sequence<T> {
        return traverseGraph(order, source, adjacents)
    }

    inline fun <T: Comparable<T>> searchAsFlow(
        order: TraversalOrder,
        source: T,
        crossinline adjacents: (T) -> Flow<T>,
    ): Flow<T> {
        return channelFlow {
            traverseGraphAsFlow(
                order,
                source,
                adjacents
            ) {
                this.send(it)
            }
        }
    }

    inline fun <T: Comparable<T>> traverseGraph(
        order: TraversalOrder,
        source: T,
        crossinline adjacents: (T) -> Sequence<T>,
    ): Sequence<T> {
        return sequence {
            val toScan = ArrayDeque<T>().apply {
                when (order) {
                    TraversalOrder.DFS -> addFirst(source)
                    TraversalOrder.BFS -> addLast(source)
                }
            }
            val visited = mutableSetOf<T>()

            while (toScan.isNotEmpty()) {
                val currentNode = toScan.removeFirst()
                yield(currentNode)
                visited.add(currentNode)

                val nextNodes = adjacents(currentNode).filterNot { visited.contains(it) }
                when (order) {
                    TraversalOrder.DFS -> nextNodes.sortedDescending().forEach { toScan.addFirst(it) }
                    TraversalOrder.BFS -> toScan.addAll(nextNodes)
                }
            }
        }
    }

    suspend inline fun <T: Comparable<T>> traverseGraphAsFlow(
        order: TraversalOrder,
        source: T,
        crossinline adjacents: (T) -> Flow<T>,
        crossinline emit: suspend (T) -> Unit,
    ) {
        val toScan = ArrayDeque<T>().apply {
            when (order) {
                TraversalOrder.DFS -> addFirst(source)
                TraversalOrder.BFS -> addLast(source)
            }
        }
        val visited = mutableSetOf<T>()

        while (toScan.isNotEmpty()) {
            val current = toScan.removeFirst()
            emit(current)
            visited.add(current)

            val nextNodes = adjacents(current).filterNot { visited.contains(it) }
            when (order) {
                TraversalOrder.DFS -> nextNodes.toList().sortedDescending().forEach { toScan.addFirst(it) }
                TraversalOrder.BFS -> toScan.addAll(nextNodes.toList())
            }
        }
    }
}
