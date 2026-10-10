package io.bluetape4k.spring.boot

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.AbstractBindHandler
import org.springframework.boot.context.properties.bind.BindContext
import org.springframework.boot.context.properties.bind.BindHandler
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.ConfigurationPropertyName
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource
import java.util.concurrent.atomic.AtomicInteger

class BinderExtensionsTest {
    class ServerProperties {
        var port: Int = 8080
    }

    private fun binder() = Binder(listOf(MapConfigurationPropertySource(mapOf("server.port" to "9090"))))

    private fun countingHandler(calls: AtomicInteger): BindHandler = object : AbstractBindHandler() {
        override fun onSuccess(
            name: ConfigurationPropertyName,
            target: Bindable<*>,
            context: BindContext,
            result: Any,
        ): Any? {
            calls.incrementAndGet()
            return result
        }
    }

    @Test
    fun `bind returns typed result and reports a missing prefix`() {
        // Given a Binder with one property under the server prefix
        val binder = binder()

        // When binding the existing and missing prefixes
        val bound = binder.bind<ServerProperties>("server")
        val missing = binder.bind<ServerProperties>("missing")

        // Then the type token is inferred and Spring reports the original binding state
        bound.get().port shouldBeEqualTo 9090
        missing.isBound.shouldBeFalse()
    }

    @Test
    fun `bindOrCreate applies bound values and keeps default values`() {
        // Given a Binder with one property under the server prefix
        val binder = binder()

        // When creating a bound object and an object for a missing prefix
        val bound = binder.bindOrCreate<ServerProperties>("server")
        val missing = binder.bindOrCreate<ServerProperties>("missing")

        // Then Spring binding and object defaults are preserved
        bound.port shouldBeEqualTo 9090
        missing.port shouldBeEqualTo 8080
    }

    @Test
    fun `handler overloads invoke the supplied BindHandler`() {
        // Given a counting handler
        val calls = AtomicInteger()
        val handler = countingHandler(calls)
        val binder = binder()

        // When binding with the handler overload
        val bound = binder.bind<ServerProperties>("server", handler)

        // Then the bind call delegates through the supplied handler
        bound.get().port shouldBeEqualTo 9090
        calls.get().shouldBeGreaterThan(0)

        // When creating with the handler overload
        calls.set(0)
        val created = binder.bindOrCreate<ServerProperties>("server", handler)

        // Then the create call delegates through the supplied handler
        created.port shouldBeEqualTo 9090
        calls.get().shouldBeGreaterThan(0)
    }

    @Test
    fun `bind accepts a nullable handler by name`() {
        // Given a Binder with a matching property
        val binder = binder()

        // When passing a null handler explicitly
        val result = binder.bind<ServerProperties>("server", bindHandler = null)

        // Then Spring binds the value using the nullable handler overload
        result.isBound.shouldBeTrue()
        result.get().port shouldBeEqualTo 9090
    }
}
