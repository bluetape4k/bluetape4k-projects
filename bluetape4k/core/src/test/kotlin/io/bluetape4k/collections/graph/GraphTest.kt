package io.bluetape4k.collections.graph

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GraphTest {

    companion object: KLogging()

    data class Node(val name: String): Comparable<Node> {
        val children = mutableListOf<Node>()

        fun addChild(child: Node): Node = apply {
            children.add(child)
        }

        override fun compareTo(other: Node): Int {
            return name.compareTo(other.name)
        }
    }

    private fun buildTree(): Node {
        return Node("root").also { root ->
            root.addChild(
                Node("child1").also { child ->
                    child.addChild(Node("grandChild11"))
                    child.addChild(Node("grandChild12"))
                }
            )
            root.addChild(
                Node("child2").also { child ->
                    child.addChild(Node("grandChild21"))
                    child.addChild(Node("grandChild22"))
                }
            )
        }
    }

    private val expectedDFS =
        listOf("root", "child1", "grandChild11", "grandChild12", "child2", "grandChild21", "grandChild22")
    private val expectedBFS =
        listOf("root", "child1", "child2", "grandChild11", "grandChild12", "grandChild21", "grandChild22")

    private lateinit var root: Node

    @BeforeEach
    fun setup() {
        root = buildTree()
    }

    @Test
    fun `verify depth first with list`() {
        val dfl = Graph.DFS.search(root) { it.children }

        val names = dfl
            .onEach { log.trace { "DFS visit node: $it" } }
            .map { it.name }
            .toList()

        names shouldBeEqualTo expectedDFS
    }

    @Test
    fun `verify depth first with sequence`() {
        val names = Graph.DFS
            .searchAsSequence(root) {
                it.children.asSequence().onEach { Thread.sleep(10) }
            }
            .onEach { log.trace { "DFS visit node: $it" } }
            .map { it.name }
            .toList()

        names shouldBeEqualTo expectedDFS
    }

    @Test
    fun `verify depth first with flow`() = runTest {
        val names = Graph.DFS
            .searchAsFlow(root) {
                it.children.asFlow().onEach { delay(10) }
            }
            // .buffer()
            .onEach { log.trace { "DFS visit node: $it" } }
            .map { it.name }
            .flowOn(Dispatchers.Default)
            .toList()

        names shouldBeEqualTo expectedDFS
    }

    @Test
    fun `verify breadth first with list`() {
        val bfl = Graph.BFS.search(root) { it.children }

        val names = bfl
            .onEach { log.trace { "DFS visit node: $it" } }
            .map { it.name }
            .toList()

        names shouldBeEqualTo expectedBFS
    }

    @Test
    fun `verify breadth first with sequence`() {
        val names = Graph.BFS
            .searchAsSequece(root) {
                it.children.asSequence().onEach { Thread.sleep(10) }
            }
            .onEach { log.trace { "BFS visit node: $it" } }
            .map { it.name }
            .toList()

        names shouldBeEqualTo expectedBFS
    }

    @Test
    fun `verify breadth first with flow`() = runTest {
        val names = Graph.BFS
            .searchAsFlow(root) {
                it.children.asFlow().onEach { delay(10) }
            }
            //.buffer()
            .onEach { log.trace { "BFS visit node: $it" } }
            .map { it.name }
            .flowOn(Dispatchers.Default)
            .toList()

        names shouldBeEqualTo expectedBFS
    }
}
