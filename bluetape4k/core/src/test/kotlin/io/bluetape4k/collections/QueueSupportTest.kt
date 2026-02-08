package io.bluetape4k.collections

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class QueueSupportTest {

    @Test
    fun `arrayBlockingQueueOf initializes`() {
        val queue = arrayBlockingQueueOf(2, collections = listOf(1, 2))
        queue.size shouldBeEqualTo 2
        queue.remainingCapacity() shouldBeEqualTo 0
        queue.poll() shouldBeEqualTo 1
    }

    @Test
    fun `arrayDequeOf initializes`() {
        val deque = arrayDequeOf<String>(10)
        deque.isEmpty().shouldBeTrue()

        val deque2 = arrayDequeOf(listOf("a", "b"))
        deque2.removeFirst() shouldBeEqualTo "a"
        deque2.removeFirst() shouldBeEqualTo "b"
    }

    @Test
    fun `concurrentLinkedQueueOf initializes`() {
        val queue = concurrentLinkedQueueOf(listOf("a", "b"))
        queue.poll() shouldBeEqualTo "a"
        queue.poll() shouldBeEqualTo "b"
    }

    @Test
    fun `linkedBlocking queues initialize`() {
        val deque = linkedBlokcingDequeOf<String>(2)
        deque.remainingCapacity() shouldBeEqualTo 2

        val deque2 = linkedBlokcingDequeOf(listOf("a", "b"))
        deque2.size shouldBeEqualTo 2

        val queue = linkedBlokcingQueueOf<Int>(3)
        queue.remainingCapacity() shouldBeEqualTo 3

        val queue2 = linkedBlokcingQueueOf(listOf(1, 2))
        queue2.size shouldBeEqualTo 2
    }

    @Test
    fun `priority queues respect ordering`() {
        val pq = priorityQueueOf<Int>(10)
        pq.addAll(listOf(3, 1, 2))
        pq.poll() shouldBeEqualTo 1

        val rpq = priorityQueueOf(10, Comparator.reverseOrder<Int>())
        rpq.addAll(listOf(3, 1, 2))
        rpq.poll() shouldBeEqualTo 3

        val pbq = priorityBlockingQueueOf<Int>(10)
        pbq.addAll(listOf(3, 1, 2))
        pbq.poll() shouldBeEqualTo 1
    }

    @Test
    fun `synchronousQueue has no capacity`() {
        val queue = synchronousQueueOf<String>()
        queue.remainingCapacity() shouldBeEqualTo 0
        queue.isEmpty().shouldBeTrue()
    }
}
