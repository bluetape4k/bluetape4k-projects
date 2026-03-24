package io.bluetape4k.collections.graph

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList

/**
 * Graph 타입을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 * - 탐색 순서의 결정성(determinism)을 보장하기 위해 노드 타입 T에 [Comparable] 제약이 필요합니다.
 *
 * ```kotlin
 * val type = Graph::class
 * // type.simpleName
 * // type.simpleName != null
 * ```
 */
object Graph {

    enum class TraversalOrder {
        DFS,
        BFS
    }

    /**
     * DFS/BFS 방식으로 그래프를 탐색하여 방문한 노드 목록을 반환합니다.
     *
     * 탐색 순서의 결정성(determinism)을 보장하기 위해 Comparable 제약이 필요합니다.
     *
     * @param T 노드 타입 ([Comparable] 제약: 탐색 순서의 결정성(determinism)을 보장하기 위해 필요)
     */
    fun <T: Comparable<T>> search(
        order: TraversalOrder,
        source: T,
        adjacents: (T) -> Iterable<T>,
    ): List<T> {
        return traverseGraph(order, source) { adjacents(it).asSequence() }.toList()
    }

    /**
     * DFS/BFS 방식으로 그래프를 탐색하여 방문한 노드의 [Sequence]를 반환합니다.
     *
     * 탐색 순서의 결정성(determinism)을 보장하기 위해 Comparable 제약이 필요합니다.
     *
     * @param T 노드 타입 ([Comparable] 제약: 탐색 순서의 결정성(determinism)을 보장하기 위해 필요)
     */
    fun <T: Comparable<T>> searchAsSequence(
        order: TraversalOrder,
        source: T,
        adjacents: (T) -> Sequence<T>,
    ): Sequence<T> {
        return traverseGraph(order, source, adjacents)
    }

    /**
     * DFS/BFS 방식으로 그래프를 탐색하여 방문한 노드의 [Flow]를 반환합니다.
     *
     * 탐색 순서의 결정성(determinism)을 보장하기 위해 Comparable 제약이 필요합니다.
     *
     * @param T 노드 타입 ([Comparable] 제약: 탐색 순서의 결정성(determinism)을 보장하기 위해 필요)
     */
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

    /**
     * DFS/BFS 방식으로 그래프를 순회하는 [Sequence]를 반환합니다.
     *
     * 탐색 순서의 결정성(determinism)을 보장하기 위해 Comparable 제약이 필요합니다.
     *
     * @param T 노드 타입 ([Comparable] 제약: 탐색 순서의 결정성(determinism)을 보장하기 위해 필요)
     */
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
            val visited = mutableSetOf(source)

            while (toScan.isNotEmpty()) {
                val currentNode = toScan.removeFirst()
                yield(currentNode)

                val nextNodes = adjacents(currentNode).filter { visited.add(it) }
                when (order) {
                    TraversalOrder.DFS -> nextNodes.toList().sortedDescending().forEach { toScan.addFirst(it) }
                    TraversalOrder.BFS -> nextNodes.forEach { toScan.addLast(it) }
                }
            }
        }
    }

    /**
     * DFS/BFS 방식으로 그래프를 순회하며 각 노드를 [emit]으로 방출합니다.
     *
     * 탐색 순서의 결정성(determinism)을 보장하기 위해 Comparable 제약이 필요합니다.
     *
     * @param T 노드 타입 ([Comparable] 제약: 탐색 순서의 결정성(determinism)을 보장하기 위해 필요)
     */
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
        val visited = mutableSetOf(source)

        while (toScan.isNotEmpty()) {
            val current = toScan.removeFirst()
            emit(current)

            val nextNodes = adjacents(current).filter { visited.add(it) }.toList()
            when (order) {
                TraversalOrder.DFS -> nextNodes.sortedDescending().forEach { toScan.addFirst(it) }
                TraversalOrder.BFS -> nextNodes.forEach { toScan.addLast(it) }
            }
        }
    }
}
