package io.nats.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.publish
import io.bluetape4k.support.toUtf8String
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class RequestReplyExample: AbstractNatsTest() {

    companion object: KLogging()

    @Test
    fun `request and reply`() {
        getConnection().use { nc ->

            // ### Reply
            // Create a message dispatcher for handling messages in a
            // separate thread and then subscribe to the target subject
            // which leverages a wildcard `greet.*`.
            // When a user makes a "request", the client populates
            // the reply-to field and then listens (subscribes) to that
            // as a subject.
            // The replier simply publishes a message to that reply-to.
            val dispatcher = nc.createDispatcher { msg ->
                val name = msg.subject.substring(6)
                val response = "hello " + name
                nc.publish(msg.replyTo, response)
            }
            dispatcher.subscribe("greet.*")

            // ### Request
            // Make a request and wait a most 1 second for a response.
            val m = nc.request("greet.bob", null, Duration.ofSeconds(1))
            log.debug { "Response received: ${m.data.toUtf8String()}" }  // hello bob

            try {
                val future = nc.request("greet.pam", null)
                val m1 = future.get(1, TimeUnit.SECONDS)
                log.debug { "Response received: ${m1.data.toUtf8String()}" } // hello pam
            } catch (e: ExecutionException) {
                log.error(e) { "Somthing went wrong with the execution of the request." }
            } catch (e: TimeoutException) {
                log.error(e) { "We didn't get a response in time." }
            } catch (e: CancellationException) {
                log.error(e) { "The request was cancelled due to no responders." }
            }

            // Once we unsubscribe there will be no subscriptions to reply.
            dispatcher.unsubscribe("greet.*")

            // If there are no-responders to a synchronous request
            // we just time out and get a null response.
            val m2 = nc.request("greet.fred", null, Duration.ofMillis(300))
            log.debug { "Response was null? ${m2 == null}" }

            // If there are no-responders to an asynchronous request
            // we get a cancellation exception.
            try {
                val future = nc.request("greet.sue", null)
                val m3 = future.get(1, TimeUnit.SECONDS)
                log.debug { "Response received: ${m3.data.toUtf8String()}" } // hello pam
            } catch (e: ExecutionException) {
                log.error(e) { "Somthing went wrong with the execution of the request." }
            } catch (e: TimeoutException) {
                log.error(e) { "We didn't get a response in time." }
            } catch (e: CancellationException) {
                log.error(e) { "The request was cancelled due to no responders." }
            }
        }
    }
}
