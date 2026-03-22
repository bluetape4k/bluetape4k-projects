package io.bluetape4k.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap

class XXHasherTest {

    companion object: KLogging()

    @Test
    fun `primitive 값의 해시가 충돌하지 않아야 한다`() {
        val v12 = XXHasher.hash(1, 2)
        val v21 = XXHasher.hash(2, 1)
        val v22 = XXHasher.hash(2, 2)

        v12 shouldNotBeEqualTo v21
        v12 shouldNotBeEqualTo v22
        v21 shouldNotBeEqualTo v22
    }

    @Test
    fun `null 포함 해시가 정상 계산되어야 한다`() {
        val h1 = XXHasher.hash("1", null)
        val h2 = XXHasher.hash(null, "1")
        log.debug { "h1=$h1, h2=$h2" }
        h1 shouldNotBeEqualTo h2
    }

    @Test
    fun `같은 입력은 같은 해시값을 반환해야 한다`() {
        val h1 = XXHasher.hash(1, "hello", 3.14)
        val h2 = XXHasher.hash(1, "hello", 3.14)
        h1 shouldBeEqualTo h2
    }

    @Test
    fun `다른 입력은 다른 해시값을 반환해야 한다`() {
        val h1 = XXHasher.hash(1, 0)
        val h2 = XXHasher.hash(0, 31)
        log.debug { "h1=$h1, h2=$h2" }
        h1 shouldNotBeEqualTo h2
    }

    @Test
    fun `빈 입력의 해시값은 0이어야 한다`() {
        XXHasher.hash() shouldBeEqualTo 0
    }

    @Test
    fun `0부터 99까지 정수 쌍의 해시가 충돌하지 않아야 한다`() {
        val hashSet = mutableSetOf<Int>()
        for (i in 0..99) {
            for (j in 0..99) {
                val hash = XXHasher.hash(i, j)
                hashSet.add(hash)
            }
        }
        hashSet.size shouldBeEqualTo 10000
    }

    @RepeatedTest(3)
    fun `concurrent hash computation should be thread-safe`() {
        val results = ConcurrentHashMap<Int, Int>()
        val threads = (1..10).map { threadId ->
            Thread {
                repeat(1000) { i ->
                    val hash = XXHasher.hash(threadId, i)
                    results[threadId * 10000 + i] = hash
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // 각 (threadId, i) 쌍에 대해 해시값 재계산하여 일치 확인
        results.forEach { (key, expectedHash) ->
            val threadId = key / 10000
            val i = key % 10000
            XXHasher.hash(threadId, i) shouldBeEqualTo expectedHash
        }
    }
}
