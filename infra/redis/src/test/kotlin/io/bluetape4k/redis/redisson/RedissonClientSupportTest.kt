package io.bluetape4k.redis.redisson

import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.redisson.api.TransactionOptions
import org.redisson.client.codec.IntegerCodec
import org.redisson.transaction.TransactionException
import java.io.Serializable
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@RandomizedTest
class RedissonClientSupportTest: AbstractRedissonTest() {

    companion object: KLogging()

    @Test
    fun `connect to redis server`() {
        val along = redisson.getAtomicLong(randomName())

        along.set(0)
        along.get() shouldBeEqualTo 0L

        along.addAndGet(5L) shouldBeEqualTo 5L
        along.addAndGet(-3L) shouldBeEqualTo 2L

        along.delete()
    }

    data class Envelope(
        val id: Long,
        val content: String,
    ): Serializable

    @Test
    fun `use redisson bloomfilter`(
        @RandomValue(type = Envelope::class) envelopes: List<Envelope>,
        @RandomValue excludeEnvelop: Envelope,
    ) {
        val bloomFilter = redisson.getBloomFilter<Envelope>(randomName())

        bloomFilter.tryInit(55_000_000L, 0.01)

        // BloomFilter에 요소를 추가한다
        envelopes.forEach {
            bloomFilter.add(it)
        }

        // BloomFilter로 요소가 존재하는지 판단한다
        envelopes.forEach {
            bloomFilter.contains(it).shouldBeTrue()
        }

        // 존재하지 않는 요소는 false 를 반환한다
        bloomFilter.contains(excludeEnvelop).shouldBeFalse()

        bloomFilter.delete()
    }

    @Test
    fun `use hyperloglog`() {
        val log = redisson.getHyperLogLog<Int>(randomName(), IntegerCodec())

        log.addAll(listOf(1, 2, 2, 2, 3, 3))
        log.count() shouldBeEqualTo 3

        log.delete()
    }

    @Test
    fun `use redis transaction`() {
        val transaction = redisson.createTransaction(TransactionOptions.defaults())
        try {
            val map = transaction.getMap<String, String>(randomName(), RedissonCodecs.String)

            map["1"] = "2"
            val value = map["3"]

            val set = transaction.getSet<String>(randomName(), RedissonCodecs.String)
            set.add(value.orEmpty())

            transaction.commit()
        } catch (e: TransactionException) {
            log.warn(e) { "Fail to transaction." }
            transaction.rollback()
        }
    }

    @Test
    fun `use pipeline by RBatch`() {
        val mapName = randomName()
        val map = redisson.getMap<String, String>(mapName)

        val batch = redisson.createBatch()
        with(batch.getMap<String, String>(mapName)) {
            fastPutAsync("1", "2")
            putAsync("2", "5")
            getAllAsync(setOf("1", "2"))
        }
        val result = batch.execute()

        log.debug { "responses=${result.responses}" }
        result.responses.last() shouldBeEqualTo mapOf("1" to "2", "2" to "5")

        map.delete()
    }

    @Test
    fun `acquire lock`() {
        val lockname = randomName()
        val lock = redisson.getFairLock(lockname)

        lock.tryLock(1, 3, TimeUnit.SECONDS).shouldBeTrue()
        // 같은 Thread 에서 tryLock 을 중복 호출하면 이미 Lock이 획득한 경우에는 True 를 반환한다
        lock.tryLock(1, 3, TimeUnit.SECONDS).shouldBeTrue()

        thread {
            // 이미 Lock 이 획득되었기 때문에 다른 ThreadId로 lock을 획득하지 못합니다.
            val lock2 = redisson.getFairLock(lockname)
            lock2.tryLock(1, 3, TimeUnit.SECONDS).shouldBeFalse()
        }.join()

        lock.unlock()
    }
}
