package io.bluetape4k.kafka.logback.keyprovider

import io.bluetape4k.assertions.shouldBeNull
import org.junit.jupiter.api.Test

class NullKafkaKeyProviderTest: AbstractKafkaKeyProviderTest() {

    override val keyProvider = NullKafkaKeyProvider()

    @Test
    fun `get null key with any event`() {
        keyProvider.get("value").shouldBeNull()
        keyProvider.get(123).shouldBeNull()
    }

    @Test
    fun `get null key with logging event`() {
        keyProvider.get(sampleEvent).shouldBeNull()
    }
}
