package io.bluetape4k.quarkus.tests.resources

import io.bluetape4k.logging.KLogging
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test

@QuarkusTest
@QuarkusTestResource(KafkaTestResource::class)
class KafkaTestResourceTest {

    companion object: KLogging()

    @Test
    fun `check kafka test resource lifecycle`() {
        KafkaTestResource.bootstrapServers.shouldNotBeEmpty()
        KafkaTestResource.kafka.isRunning.shouldBeTrue()
    }
}
