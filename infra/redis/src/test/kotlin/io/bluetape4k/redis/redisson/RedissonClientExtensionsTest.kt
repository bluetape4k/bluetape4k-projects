package io.bluetape4k.redis.redisson

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RedissonClientExtensionsTest: AbstractRedissonTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `batch operation`() {
        val mapName = randomName()

        val value1 = randomName()
        val value2 = randomName()
        val result = redisson.withBatch {
            val batchMap = getMap<String, String>(mapName)
            batchMap.fastPutAsync("key1", value1)
            batchMap.putAsync("key2", value2)
            batchMap.getAllAsync(setOf("key1", "key2"))
        }

        log.debug { "result=${result.responses}" }

        result.responses[0] shouldBeEqualTo true
        result.responses[1].shouldBeNull()
        result.responses[2] shouldBeEqualTo mapOf("key1" to value1, "key2" to value2)

        redisson.getMap<String, String>(mapName).delete()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `transaction operation`() {
        val mapName = randomName()
        val setName = randomName()

        redisson.withTransaction {
            val map = getMap<String, String>(mapName)
            map["1"] = randomName()
            val value = map["3"]

            val set = getSet<String>(setName)
            set.add(value ?: randomName())
        }

        redisson.getMap<String, String>(mapName).delete()
        redisson.getSet<String>(setName).delete()
    }

    @Test
    fun `transaction should rollback when action throws runtime exception`() {
        val mapName = randomName()
        val map = redisson.getMap<String, String>(mapName)

        try {
            assertThrows<IllegalStateException> {
                redisson.withTransaction {
                    val txMap = getMap<String, String>(mapName)
                    txMap["1"] = "value"
                    throw IllegalStateException("boom")
                }
            }

            map["1"].shouldBeNull()
        } finally {
            map.delete()
        }
    }
}
