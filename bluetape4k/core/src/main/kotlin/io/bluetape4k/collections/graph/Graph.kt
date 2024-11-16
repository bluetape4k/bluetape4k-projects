package io.bluetape4k.collections.graph

import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.toList

/**
 * Graph Algorithms 중 기본적인 알고리즘을 제공합니다.
 */
object Graph: KLogging() {

    /**
     * Depth First Search 알고리즘을 제공합니다.
     */
    object DFS {

        /**
         * 깊이 우선 탐색 (Depth first search) 을 통해 탐색한 요소 순서대로 List을 만든다
         * 단 acyclic 해야 합니다 (이미 탐색한 노드가 나오면 안됩니다)
         *
         * 사용 예:
         * ```
         *  private val expectedDFS =
         *         listOf("root", "child1", "grandChild11", "grandChild12", "child2", "grandChild21", "grandChild22")
         *
         *  val dfl = Graph.DFS.search(root) { it.children }
         *  val names = dfl
         *      .onEach { log.trace { "DFS visit node: $it" } }
         *      .map { it.name }
         *      .toList()
         *  names shouldBeEqualTo expectedDFS
         * ```
         *
         * @param T
         * @param source 시작 노드
         * @param adjacents 노드의 다음 탐색 노드들을 구하는 함수
         * @return 탐색 경로에 있는 노드들의 리스트
         */
        fun <T: Comparable<T>> search(source: T, adjacents: (T) -> Iterable<T>): List<T> {
            return searchAsSequence(source) { adjacents(it).asSequence() }.toList()
        }

        /**
         * 깊이 우선 탐색 (Depth first search) 을 통해 탐색한 요소 순서대로 [Sequence]를 만든다
         *
         * 사용 예:
         * ```
         * val names = Graph.DFS
         *     .searchAsSequence(root) {
         *         it.children.asSequence().onEach { Thread.sleep(10) }
         *     }
         *     .onEach { log.trace { "DFS visit node: $it" } }
         *     .map { it.name }
         *     .toList()
         * names shouldBeEqualTo expectedDFS
         * ```
         *
         * @param T
         * @param source 시작 노드
         * @param adjacents 노드의 다음 탐색 노드들을 구하는 함수
         * @return 탐색 경로에 있는 노드들의 리스트
         */
        inline fun <T: Comparable<T>> searchAsSequence(
            source: T,
            crossinline adjacents: (T) -> Sequence<T>,
        ): Sequence<T> = sequence {
            val toScan = ArrayDeque<T>().apply { addFirst(source) }
            val visited = mutableSetOf<T>()

            while (toScan.isNotEmpty()) {
                val current = toScan.removeFirst()
                yield(current)
                visited.add(current)

                adjacents(current)
                    .filterNot { visited.contains(it) }
                    .sortedDescending()
                    .forEach { toScan.addFirst(it) }
            }
        }

        /**
         * 깊이 우선 탐색 (Depth first search) 을 통해 탐색한 요소 순서대로 [Flow]를 만든다
         *
         * ```
         * val names = Graph.DFS
         *     .searchAsFlow(root) {
         *         it.children.asFlow().onEach { delay(10) }
         *     }
         *     // .buffer()
         *     .onEach { log.trace { "DFS visit node: $it" } }
         *     .map { it.name }
         *     .flowOn(Dispatchers.Default)
         *     .toList()
         * names shouldBeEqualTo expectedDFS
         * ```
         *
         * @param source 시작 노드
         * @param adjacents 노드의 다음 탐색 노드들을 구하는 함수
         * @return 탐색 경로에 있는 노드들의 리스트
         */
        inline fun <T: Comparable<T>> searchAsFlow(
            source: T,
            crossinline adjacents: (T) -> Flow<T>,
        ): Flow<T> = channelFlow {
            val toScan = ArrayDeque<T>().apply { addFirst(source) }
            val visited = mutableSetOf<T>()

            while (toScan.isNotEmpty()) {
                val current = toScan.removeFirst()
                send(current)
                visited.add(current)

                adjacents(current)
                    .filterNot { visited.contains(it) }
                    .toList()
                    .sortedDescending()
                    .forEach { toScan.addFirst(it) }
            }
        }
    }

    object BFS {

        /**
         * 폭 우선 탐색 (Breadth first search)를 통해 탐색한 요 순서대로 List를 만든다
         *
         * ```
         * private val expectedBFS =
         *         listOf("root", "child1", "child2", "grandChild11", "grandChild12", "grandChild21", "grandChild22")
         *
         * val bfl = Graph.BFS.search(root) { it.children }
         * val names = bfl
         *     .onEach { log.trace { "DFS visit node: $it" } }
         *     .map { it.name }
         *     .toList()
         * names shouldBeEqualTo expectedBFS
         * ```
         *
         * @param T
         * @param source 시작 노드
         * @param adjacents 노드의 다음 탐색 노드들을 구하는 함수
         * @return 탐색 경로에 있는 노드들의 리스트
         */
        fun <T: Comparable<T>> search(source: T, adjacents: (T) -> Iterable<T>): List<T> {
            return searchAsSequece(source) { adjacents(it).asSequence() }.toList()
        }

        /**
         * 폭 우선 탐색 (Breadth first search)를 통해 탐색한 요 순서대로 Sequence를 만든다
         *
         * 사용 예:
         * ```
         * val names = Graph.BFS
         *     .searchAsSequece(root) {
         *         it.children.asSequence().onEach { Thread.sleep(10) }
         *     }
         *     .onEach { log.trace { "BFS visit node: $it" } }
         *     .map { it.name }
         *     .toList()
         * names shouldBeEqualTo expectedBFS
         * ```
         *
         * @param source 시작 노드
         * @param adjacents 노드의 다음 탐색 노드들을 구하는 함수
         * @return 탐색 경로에 있는 노드들의 리스트
         */
        inline fun <T: Comparable<T>> searchAsSequece(
            source: T,
            crossinline adjacents: (T) -> Sequence<T>,
        ): Sequence<T> = sequence {
            val toScan = ArrayDeque<T>().apply { addLast(source) }
            val visited = mutableSetOf<T>()

            while (toScan.isNotEmpty()) {
                val current = toScan.removeFirst()
                yield(current)
                visited.add(current)

                adjacents(current)
                    .filterNot { visited.contains(it) }
                    .apply { toScan.addAll(this) }
                // .sorted()
                // .forEach { toScan.addLast(it) }
            }
        }

        /**
         * 폭 우선 탐색 (Breadth first search) 을 통해 탐색한 요소 순서대로 [Flow]를 만든다
         *
         * 사용 예:
         * ```
         * val names = Graph.BFS
         *     .searchAsFlow(root) {
         *         it.children.asFlow().onEach { delay(10) }
         *     }
         *     //.buffer()
         *     .onEach { log.trace { "BFS visit node: $it" } }
         *     .map { it.name }
         *     .flowOn(Dispatchers.Default)
         *     .toList()
         * names shouldBeEqualTo expectedBFS
         * ```
         *
         * @param source 시작 노드
         * @param adjacents 노드의 다음 탐색 노드들을 구하는 함수
         * @return 탐색 경로에 있는 노드들의 리스트
         */
        inline fun <T: Comparable<T>> searchAsFlow(
            source: T,
            crossinline adjacents: (T) -> Flow<T>,
        ): Flow<T> = channelFlow {
            val toScan = ArrayDeque<T>().apply { addLast(source) }
            val visited = mutableSetOf<T>()

            while (toScan.isNotEmpty()) {
                val current = toScan.removeFirst()
                send(current)
                visited.add(current)

                adjacents(current)
                    .filterNot { visited.contains(it) }
                    .toList()
                    .apply { toScan.addAll(this) }
                //.sorted()
                // .forEach { toScan.addLast(it) }
            }
        }
    }
}
