package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class SpscArrayQueueTest {

    companion object: KLogging()

    @Test
    fun offerPoll() {
        val q = SpscArrayQueue<Int>(10)
        val a = Array<Any?>(1) { 0 }

        repeat(10) {
            q.offer(it).shouldBeTrue()
            q.isEmpty.shouldBeFalse()
            q.poll(a).shouldBeTrue()
            q.isEmpty.shouldBeTrue().shouldBeTrue()
            a[0] shouldBeEqualTo it
        }
    }

    @Test
    fun offerAllPollAll() {
        val q = SpscArrayQueue<Int>(10)
        val a = Array<Any?>(1) { 0 }

        repeat(16) {
            q.offer(it).shouldBeTrue()
            q.isEmpty.shouldBeFalse()
        }

        q.offer(16).shouldBeFalse()

        repeat(16) {
            q.isEmpty.shouldBeFalse()
            q.poll(a).shouldBeTrue()
            a[0] shouldBeEqualTo it
        }

        q.isEmpty.shouldBeTrue()
        q.poll(a).shouldBeFalse()
        q.isEmpty.shouldBeTrue()
    }

    @Test
    fun clear() {
        val q = SpscArrayQueue<Int>(16)
        val a = Array<Any?>(1) { 0 }

        for (i in 0 until 10) {
            q.offer(i)
        }

        q.clear()
        q.isEmpty.shouldBeTrue()

        repeat(16) {
            q.offer(it).shouldBeTrue()
        }

        q.offer(16).shouldBeFalse()


        repeat(16) {
            q.isEmpty.shouldBeFalse()
            q.poll(a).shouldBeTrue()
            a[0] shouldBeEqualTo it
        }

        q.isEmpty.shouldBeTrue()
        q.poll(a).shouldBeFalse()
        q.isEmpty.shouldBeTrue()
    }
}
