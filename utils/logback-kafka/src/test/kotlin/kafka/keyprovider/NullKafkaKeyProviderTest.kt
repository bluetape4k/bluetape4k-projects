package io.bluetape4k.logback.kafka.keyprovider

import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test

class NullKafkaKeyProviderTest: AbstractKafkaKeyProviderTest() {

    override val keyProvider = NullKafkaKeyProvider()

    @Test
    fun `get null key with null event`() {
        keyProvider.get(null).shouldBeNull()
    }

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
