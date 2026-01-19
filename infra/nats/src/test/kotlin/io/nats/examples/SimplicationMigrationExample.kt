package io.nats.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.api.consumerConfiguration
import io.bluetape4k.nats.client.api.streamConfiguration
import io.bluetape4k.nats.client.pullSubscriptionOptions
import io.nats.client.JetStreamOptions
import io.nats.client.JetStreamStatusCheckedException
import io.nats.client.JetStreamSubscription
import io.nats.client.PushSubscribeOptions
import io.nats.client.api.AckPolicy
import io.nats.client.api.ConsumerConfiguration
import io.nats.client.api.StorageType
import io.nats.client.api.StreamInfoOptions
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * This example will demonstrate migrating from current api to simplification api
 * SIMPLIFICATION IS EXPERIMENTAL AND SUBJECT TO CHANGE
 */
class SimplicationMigrationExample: AbstractNatsTest() {

    companion object: KLogging()

    @Test
    fun `simplification migration`() {
        getConnection().use { nc ->
            // ## Legacy JetStream API
            //
            // The legacy JetStream API provides two contexts both created from the Connection.
            // The `JetStream` context provides the ability to publish to streams and subscribe
            // to streams (via consumers). The `JetStreamManagement` context provides the ability
            // to manage streams and consumers themselves.
            val js = nc.jetStream()
            val jsm = nc.jetStreamManagement(JetStreamOptions.DEFAULT_JS_OPTIONS)

            // Create a stream and populate the stream with a few messages.
            val streamName = "migration"
            jsm.addStream(streamConfiguration {
                name(streamName)
                storageType(StorageType.Memory)
                subjects("events.>")
                duplicateWindow(Duration.ofSeconds(1))
            })

            js.publish("events.1", null)
            js.publish("events.2", null)
            js.publish("events.3", null)

            // ### Continuous message retrieval with `subscribe()`
            //
            // Using the `JetStream` context, the common way to continuously receive messages is
            // to use push consumers.
            // The easiest way to create a consumer and start consuming messages
            // using the JetStream context is to use the `subscribe()` method. `subscribe()`,
            // while familiar to core NATS users, leads to complications because it will
            // create underlying consumers if they don't already exist.
            println()
            println("A. Legacy Push Subscription with Ephemeral Consumer")
            println("  Async")

            val dispatcher = nc.createDispatcher()

            // By default, `subscribe()` performs a stream lookup by subject.
            // You can save a lookup to the server by providing the stream name in the subscribe options
            val pushSubscriptionOptions = PushSubscribeOptions.stream(streamName)

            val asyncSub: JetStreamSubscription = js.subscribe(
                "events.>",
                dispatcher,
                { msg ->
                    println("    Received: ${msg.subject}")
                },
                false,
                pushSubscriptionOptions
            )
            Thread.sleep(100)

            // Unsubscribing this subscription will result in the underlying
            // ephemeral consumer being deleted proactively on the server.
            dispatcher.unsubscribe(asyncSub)

            println("  Sync")
            val syncSub = js.subscribe("events.>", pushSubscriptionOptions)
            while (true) {
                val msg = syncSub.nextMessage(100L) ?: break
                println("    Read: ${msg.subject}")
                msg.ack()
            }
            syncSub.unsubscribe()

            // ### Binding to an existing consumer
            //
            // In order to create a consumer outside the `subscribe` method,
            // the `JetStreamManagement` context `addOrUpdateConsumer` method can be used.
            // If a durable is not provided, the consumer will be ephemeral and will
            // be deleted if it becomes inactive for longer than the inactivity threshold.
            // If neither `durable` nor `name` are not provided, the client will generate a name
            // that can be found via `ConsumerInfo.getName()`
            println("\nB. Legacy Bind Subscription to Named Consumer.")
            var consumerConfiguration = consumerConfiguration {
                deliverSubject("deliverB")
                ackPolicy(AckPolicy.Explicit)
                inactiveThreshold(Duration.ofMinutes(10))
            }

            var consumerInfo = jsm.addOrUpdateConsumer(streamName, consumerConfiguration)
            val sub = js.subscribe(
                null,
                dispatcher,
                { msg ->
                    println("   Received: ${msg.subject}")
                    msg.ack()
                },
                false,
                PushSubscribeOptions.bind(streamName, consumerInfo.name)
            )
            Thread.sleep(100)
            dispatcher.unsubscribe(sub)

            // ### Pull consumers
            //
            // The `JetStream` context API also supports pull consumers.
            // Using pull consumers requires more effort on the developer's side
            // than push consumers to maintain an endless stream of messages.
            // Batches of messages can be retrieved using the `iterate` method.
            // Iterate will start retrieving messages from the server as soon as
            // it is called but returns right away (does not block) so you can
            // start handling messages as soon as the first one comes from the server.
            println("\nC. Legacy Pull Subscription then Iterate")
            val pullSubscriptionOptions = pullSubscriptionOptions { }
            val pullSub: JetStreamSubscription = js.subscribe("events.>", pullSubscriptionOptions)

            var start = System.currentTimeMillis()
            val iterator = pullSub.iterate(10, 2000)

            var elapsed = System.currentTimeMillis() - start
            println("    The call to `iterate(10, 2000)` returned in $elapsed ms.")

            while (iterator.hasNext()) {
                val msg = iterator.next()
                elapsed = System.currentTimeMillis() - start
                println("    Processing ${msg.subject} $elapsed ms after start.")
                msg.ack()
            }
            elapsed = System.currentTimeMillis() - start
            println("    The iterate completed in $elapsed ms.")
            println("      Time reflects waiting for the entire batch, which isn't available")

            // ## Simplified JetStream API
            //
            // The simplified API has a `StreamContext` for accessing existing
            // streams, creating consumers, and getting a `ConsumerContext`.
            // The `StreamContext` can be created from the `Connection` similar to
            // the legacy API.
            println("\nD. Simplicafication StreamContext")
            val streamContext = nc.getStreamContext(streamName)
            val streamInfo = streamContext.getStreamInfo(StreamInfoOptions.allSubjects())

            println("    Stream Name: ${streamInfo.configuration.name}")
            println("    Stream Subjects: ${streamInfo.streamState.subjects}")

            // ### Creating a consumer from the stream context
            //
            // To create an ephemeral consumer, the `createOrUpdateConsumer` method
            // can be used with a bare `ConsumerConfiguration` object.
            println("\nE. Simplification, Create a Consumer")
            consumerConfiguration = ConsumerConfiguration.builder().build()
            val consumercontext = streamContext.createOrUpdateConsumer(consumerConfiguration)
            consumerInfo = consumercontext.cachedConsumerInfo
            val consumerName = consumerInfo.name

            println("    A consumer was created on stream `${consumerInfo.streamName}` ")
            println("    The consumer name is `${consumerInfo.name}`")
            println("    The consumer has `${consumerInfo.numPending}` messages available.")

            // ### Getting a consumer from the stream context
            //
            // If your consumer already exists as a durable, you can create a
            // `ConsumerContext` for that consumer from the stream context or directly
            // from the connection by providing the stream and consumer name.
            var consumerContext = streamContext.getConsumerContext(consumerName)
            consumerInfo = consumerContext.cachedConsumerInfo
            println("    The ConsumerContext for `${consumerName}` was loaded from the StreamContext for `${consumerInfo.streamName}`")

            consumerContext = nc.getConsumerContext(streamName, consumerName)
            consumerInfo = consumerContext.cachedConsumerInfo
            println("    The ConsumerContext for `${consumerName}` was loaded from the Connection on the stream `${consumerInfo.streamName}`")

            // ### Continuous message retrieval with `consume()`
            //
            // In order to continuously receive messages, the `consume` method
            // can be used with or without a `MessageHandler`. These methods work
            // similarly to the push `subscribe` methods used to receive messages.
            //
            // `consume` (and other ConsumerContext methods) never create a consumer
            // instead always using a consumer created previously.
            // <!break>


            // #### MessageConsumer
            // A `MessageConsumer` is returned when you call the `consume` method passing
            // `MessageHandler` on `ConsumerContext`.
            // Auto *ack* is no longer an option when a handler is provided to avoid
            // confusion. It is the developer's responsibility to ack or not based on
            // the consumer's ack policy. Ack policy is "explicit" if not otherwise set.
            //
            // Remember, when you have a handler and message are sent asynchronously,
            // make sure you have set up your error handler.
            println("\nF. MessageConsumer (endless consumer with handler)")
            consumerConfiguration = consumerConfiguration { }
            consumerContext = streamContext.createOrUpdateConsumer(consumerConfiguration)
            consumerInfo = consumerContext.cachedConsumerInfo

            println("    A consumer was created on stream `${consumerInfo.streamName}`")
            println("    The consumer name is `${consumerInfo.name}`.")
            println("    The consumer has `${consumerInfo.numPending}` messages available.")

            val messageConsumer = consumerContext.consume { msg ->
                println("    Received: ${msg.subject}")
                msg.ack()
            }
            Thread.sleep(100)

            // To stop the consumer, the `stop` on `MessageConsumer` can be used.
            // In contrast to `unsubscribe()` in the legacy API, this will not proactively
            // delete the consumer.
            // However, the consumer will be automatically deleted by the server when the
            // `inactiveThreshold` is reached.

            // To stop the consumer, the `stop` on `MessageConsumer` can be used.
            // In contrast to `unsubscribe()` in the legacy API, this will not proactively
            // delete the consumer.
            // However, the consumer will be automatically deleted by the server when the
            // `inactiveThreshold` is reached.
            messageConsumer.stop()
            println("   stop was called.")

            // #### IterableConsumer
            // An `IterableConsumer` is returned when you call the `consume` method on
            // the `ConsumerContext` *without* supplying a message handler.
            println("\nG. IterableConsumer (endless consumer manually calling next)")
            consumerConfiguration = consumerConfiguration { }
            consumerContext = streamContext.createOrUpdateConsumer(consumerConfiguration)
            consumerInfo = consumerContext.cachedConsumerInfo

            println("    A consumer was created on stream `${consumerInfo.streamName}`")
            println("    The consumer name is `${consumerInfo.name}`.")
            println("    The consumer has `${consumerInfo.numPending}` messages available.")

            // Notice the `nextMessage` method can throw a `JetStreamStatusCheckedException`.
            // Under the covers the `IterableConsumer` is handling more than just messages.
            // It handles information from the server regarding the status of the underlying
            // operations. For instance, it is possible, but unlikely, that the consumer
            // could be deleted by another application in your ecosystem and if that happens
            // in the middle of the consumer, the exception would be thrown.
            val iterableConsumer = consumerContext.fetchMessages(100)
            try {
                for (x in 0 until 3) {
                    val msg = iterableConsumer.nextMessage()
                    println("    Received: ${msg.subject}")
                    msg.ack()
                }
            } catch (se: JetStreamStatusCheckedException) {
                println("    JetStreamStatusCheckedException: ${se.message}")
            }

            // ### Retrieving messages on demand with `fetch` and `next`

            // #### FetchConsumer
            // A `FetchConsumer` is returned when you call the `fetch` methods on `ConsumerContext`.
            // You will use that object to call `nextMessage`.
            // Notice there is no stop on the `FetchConsumer` interface, the fetch stops by itself.
            // The new version of fetch is very similar to the old iterate, as it does not block
            // before returning the entire batch.
            println("\nH. FetchConsumer (bounded consumer)")
            consumerConfiguration = consumerConfiguration { }
            consumerContext = streamContext.createOrUpdateConsumer(consumerConfiguration)
            consumerInfo = consumerContext.cachedConsumerInfo

            println("    A consumer was created on stream `${consumerInfo.streamName}`")
            println("    The consumer name is `${consumerInfo.name}`.")
            println("    The consumer has `${consumerInfo.numPending}` messages available.")

            start = System.currentTimeMillis()
            val fetchConsumer = consumerContext.fetchMessages(2)
            elapsed = System.currentTimeMillis() - start
            println("   'fetch' returned in " + elapsed + "ms.")

            // `fetch` will return null once there are no more messages to consume.

            // `fetch` will return null once there are no more messages to consume.
            try {
                var msg = fetchConsumer.nextMessage()
                while (msg != null) {
                    elapsed = System.currentTimeMillis() - start
                    println("   Processing " + msg.subject + " " + elapsed + "ms after start.")
                    msg.ack()
                    msg = fetchConsumer.nextMessage()
                }
            } catch (se: JetStreamStatusCheckedException) {
                println("   JetStreamStatusCheckedException: " + se.message)
            }
            elapsed = System.currentTimeMillis() - start
            println("   Fetch complete in " + elapsed + "ms.")

            // #### next
            // The `next` method can be used to retrieve a single
            // message, as if you had called the old fetch or iterate with a batch size of 1.
            // The minimum wait time when calling next is 1 second (1000ms)
            println("\nI. next (1 message)")
            try {
                val msg = consumerContext.next(1000)!!
                println("   Received " + msg.subject)
                msg.ack()
            } catch (se: JetStreamStatusCheckedException) {
                println("   JetStreamStatusCheckedException: " + se.message)
            }
        }
    }
}
