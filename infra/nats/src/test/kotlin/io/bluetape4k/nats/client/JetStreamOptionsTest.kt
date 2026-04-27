package io.bluetape4k.nats.client

import io.nats.client.JetStreamOptions
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Duration

class JetStreamOptionsTest {

    @Test
    fun `defaultJetStreamOptions is DEFAULT_JS_OPTIONS`() {
        defaultJetStreamOptions shouldBeEqualTo JetStreamOptions.DEFAULT_JS_OPTIONS
    }

    @Test
    fun `jetStreamOptions with empty builder creates default instance`() {
        val jso = jetStreamOptions {}

        jso.shouldNotBeNull()
    }

    @Test
    fun `jetStreamOptionsOf with no parameters creates default instance`() {
        val jso = jetStreamOptionsOf()

        jso.shouldNotBeNull()
        jso.isPublishNoAck.shouldBeFalse()
    }

    @Test
    fun `jetStreamOptionsOf with prefix applies prefix`() {
        val jso = jetStreamOptionsOf(prefix = "myprefix")

        jso.shouldNotBeNull()
        // jnats appends "." to prefix by convention
        jso.prefix shouldBeEqualTo "myprefix."
    }

    @Test
    fun `jetStreamOptionsOf with requestTimeout applies timeout`() {
        val timeout = Duration.ofSeconds(5)
        val jso = jetStreamOptionsOf(requestTimeout = timeout)

        jso.shouldNotBeNull()
        jso.requestTimeout shouldBeEqualTo timeout
    }

    @Test
    fun `jetStreamOptionsOf with publishNoAck true applies flag`() {
        val jso = jetStreamOptionsOf(publishNoAck = true)

        jso.shouldNotBeNull()
        jso.isPublishNoAck.shouldBeTrue()
    }

    @Test
    fun `jetStreamOptionsOf with all params applies all`() {
        val jso = jetStreamOptionsOf(
            prefix = "test",
            requestTimeout = Duration.ofSeconds(3),
            publishNoAck = false,
        )

        jso.shouldNotBeNull()
        jso.prefix shouldBeEqualTo "test."
        jso.requestTimeout shouldBeEqualTo Duration.ofSeconds(3)
        jso.isPublishNoAck.shouldBeFalse()
    }
}
