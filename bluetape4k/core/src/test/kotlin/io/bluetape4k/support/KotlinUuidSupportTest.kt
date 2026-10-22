package io.bluetape4k.support

import io.bluetape4k.AbstractCoreTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class KotlinUuidSupportTest: AbstractCoreTest() {

    companion object: KLogging()

    @Test
    fun `get uuid version`() {
        val uuidV4 = Uuid.generateV4()
        uuidV4.uuidVersion() shouldBeEqualTo 4

        val uuidV7 = Uuid.generateV7()
        uuidV7.uuidVersion() shouldBeEqualTo 7
    }

    @Test
    fun `get v7 timestamp`() {
        val timestamp = System.currentTimeMillis()
        Thread.sleep(10)

        val uuidV7 = Uuid.generateV7()
        uuidV7.uuidVersion() shouldBeEqualTo 7
        uuidV7.v7Timestamp() shouldBeGreaterThan timestamp

        val uuidV4 = Uuid.generateV4()
        uuidV4.uuidVersion() shouldBeEqualTo 4

        // UUID V4 의 Timestamp 는 랜덤 값이므로 비교를 할 수 없다.
        // uuidV4.v7Timestamp() shouldBeEqualTo timestamp

    }
}
