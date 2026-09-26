package io.bluetape4k.spring.redis.serializer

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes

abstract class AbstractRedisSerializerTest {

    companion object: KLogging() {
        @JvmField
        val faker = Fakers.faker
    }

    data class TestData(
        val id: Long = 0L,
        val name: String = "",
        val value: Double = 0.0,
        val description: String = "",
    ): java.io.Serializable

    protected fun newTestData() = TestData(
        id = faker.random().nextLong(),
        name = faker.name().fullName(),
        value = faker.random().nextDouble(),
        description = faker.lorem().sentence(),
    )

    protected fun newTestDataBytes() =
        faker.lorem().paragraph(5).toUtf8Bytes()
}
