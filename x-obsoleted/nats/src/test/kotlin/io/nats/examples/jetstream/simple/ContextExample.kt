package io.nats.examples.jetstream.simple

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.api.consumerConfiguration
import io.bluetape4k.nats.client.createOrReplaceStream
import io.nats.client.JetStreamOptions
import org.junit.jupiter.api.Test

class ContextExample: AbstractNatsTest() {

    companion object: KLogging() {
        private const val STREAM = "context-stream"
        private const val SUBJECT = "context-subject"
        private const val CONSUMER_NAME = "context-consumer"
    }

    @Test
    fun `retrieve jetstream context`() {
        getConnection().use { nc ->
            val js = nc.jetStream()
            nc.createOrReplaceStream(STREAM, SUBJECT)

            // get a stream context from the connection
            var streamContext = nc.getStreamContext(STREAM)
            log.debug { "S1. ${streamContext.streamInfo}" }

            // get a stream context from the connection, supplying custom JetStreamOptions
            streamContext = nc.getStreamContext(STREAM, JetStreamOptions.DEFAULT_JS_OPTIONS)
            log.debug { "S2. ${streamContext.streamInfo}" }

            // get a stream context from the JetStream context
            streamContext = js.getStreamContext(STREAM)
            log.debug { "S3. ${streamContext.streamInfo}" }

            // when you create a consumer from the stream context you get a ConsumerContext in return
            var consumerContext = streamContext.createOrUpdateConsumer(consumerConfiguration { durable(CONSUMER_NAME) })
            log.debug { "C1. ${consumerContext.cachedConsumerInfo}" }

            // get a ConsumerContext from the connection for a pre-existing consumer
            consumerContext = nc.getConsumerContext(STREAM, CONSUMER_NAME)
            log.debug { "C2. ${consumerContext.cachedConsumerInfo}" }

            // get a ConsumerContext from the connection for a pre-existing consumer, supplying custom JetStreamOptions
            consumerContext = nc.getConsumerContext(STREAM, CONSUMER_NAME, JetStreamOptions.DEFAULT_JS_OPTIONS)
            log.debug { "C3. ${consumerContext.cachedConsumerInfo}" }

            // get a ConsumerContext from the stream context for a pre-existing consumer
            consumerContext = streamContext.getConsumerContext(CONSUMER_NAME)
            log.debug { "C4. ${consumerContext.cachedConsumerInfo}" }
        }
    }
}
