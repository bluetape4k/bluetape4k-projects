package io.bluetape4k.r2dbc.pool

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.r2dbc.spi.Closeable as R2dbcCloseable
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryMetadata
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.concurrent.atomic.AtomicInteger

class R2dbcConnectionFactoryRegistryTest {

    @Test
    fun `borrowed registry는 lookup과 immutable snapshot을 제공한다`() {
        val factory = FakeConnectionFactory("primary")
        val source = linkedMapOf("primary" to factory)
        val registry = R2dbcConnectionFactoryRegistry.borrowed(source)

        source["late"] = FakeConnectionFactory("late")

        registry.keys shouldBeEqualTo setOf("primary")
        registry["primary"] shouldBeSameInstanceAs factory
        registry.asMap()["primary"] shouldBeSameInstanceAs factory
        registry.routingMap(String::uppercase)["PRIMARY"] shouldBeSameInstanceAs factory

        registry.close().block()
        factory.closeCalls.get() shouldBeEqualTo 0
    }

    @Test
    fun `unknown key는 configured key를 노출하지 않고 fail fast한다`() {
        val registry = R2dbcConnectionFactoryRegistry.borrowed(
            mapOf("primary" to FakeConnectionFactory("primary")),
        )

        val failure = assertFailsWith<NoSuchElementException> {
            registry["missing"]
        }

        failure.message shouldBeEqualTo "No ConnectionFactory configured for the requested key."
    }

    @Test
    fun `routing map의 mapped key 충돌을 거부한다`() {
        val registry = R2dbcConnectionFactoryRegistry.borrowed(
            linkedMapOf(
                "one" to FakeConnectionFactory("one"),
                "two" to FakeConnectionFactory("two"),
            ),
        )

        assertFailsWith<IllegalArgumentException> {
            registry.routingMap { "same" }
        }
    }

    @Test
    fun `같은 owned resource alias는 한 번만 종료한다`() {
        val factory = FakeConnectionFactory("shared")
        val registry = R2dbcConnectionFactoryRegistry(
            linkedMapOf(
                "one" to R2dbcConnectionFactoryEntry.owned(factory),
                "two" to R2dbcConnectionFactoryEntry.owned(factory),
            ),
        )

        registry.close().block()
        registry.dispose()

        factory.closeCalls.get() shouldBeEqualTo 1
    }

    @Test
    fun `borrowed와 owned alias 혼합을 생성 시 거부한다`() {
        val factory = FakeConnectionFactory("shared")

        assertFailsWith<IllegalArgumentException> {
            R2dbcConnectionFactoryRegistry(
                linkedMapOf(
                    "borrowed" to R2dbcConnectionFactoryEntry.borrowed(factory),
                    "owned" to R2dbcConnectionFactoryEntry.owned(factory),
                ),
            )
        }
    }

    @Test
    fun `close 이후 lookup과 map access를 거부한다`() {
        val registry = R2dbcConnectionFactoryRegistry.borrowed(
            mapOf("primary" to FakeConnectionFactory("primary")),
        )
        registry.close().block()

        assertFailsWith<IllegalStateException> { registry["primary"] }
        assertFailsWith<IllegalStateException> { registry.asMap() }
        assertFailsWith<IllegalStateException> { registry.routingMap { it } }
    }

    @Test
    fun `close는 모든 오류를 시도하고 후속 오류를 suppressed로 보존한다`() {
        val first = IllegalStateException("first close failure")
        val second = IllegalArgumentException("second close failure")
        val firstCalls = AtomicInteger()
        val secondCalls = AtomicInteger()
        val firstFactory = PlainConnectionFactory("first")
        val secondFactory = PlainConnectionFactory("second")
        val registry = R2dbcConnectionFactoryRegistry(
            linkedMapOf(
                "first" to R2dbcConnectionFactoryEntry.owned(firstFactory) {
                    firstCalls.incrementAndGet()
                    Mono.error(first)
                },
                "second" to R2dbcConnectionFactoryEntry.owned(secondFactory) {
                    secondCalls.incrementAndGet()
                    Mono.error(second)
                },
            ),
        )

        registry.dispose()

        StepVerifier.create(registry.close())
            .expectErrorSatisfies { failure ->
                firstCalls.get() shouldBeEqualTo 1
                secondCalls.get() shouldBeEqualTo 1
                failure shouldBeSameInstanceAs first
                failure.suppressed shouldHaveSize 1
                failure.suppressed.first() shouldBeSameInstanceAs second
            }
            .verify()
    }

    @Test
    fun `close가 같은 Throwable identity를 반복해도 self suppression으로 실패하지 않는다`() {
        val shared = IllegalStateException("shared close failure")
        val firstFactory = PlainConnectionFactory("first")
        val secondFactory = PlainConnectionFactory("second")
        val registry = R2dbcConnectionFactoryRegistry(
            linkedMapOf(
                "first" to R2dbcConnectionFactoryEntry.owned(firstFactory) { Mono.error(shared) },
                "second" to R2dbcConnectionFactoryEntry.owned(secondFactory) { Mono.error(shared) },
            ),
        )

        StepVerifier.create(registry.close())
            .expectErrorSatisfies { failure ->
                failure shouldBeSameInstanceAs shared
                failure.suppressed shouldHaveSize 0
            }
            .verify()
    }

    @Test
    fun `concurrent lookup은 A와 B factory를 교차하지 않는다`() {
        val factoryA = FakeConnectionFactory("A")
        val factoryB = FakeConnectionFactory("B")
        val registry = R2dbcConnectionFactoryRegistry.borrowed(
            linkedMapOf("A" to factoryA, "B" to factoryB),
        )
        val counter = AtomicInteger()

        MultithreadingTester()
            .workers(8)
            .rounds(30)
            .add {
                if (counter.incrementAndGet() % 2 == 0) {
                    registry["A"] shouldBeSameInstanceAs factoryA
                } else {
                    registry["B"] shouldBeSameInstanceAs factoryB
                }
            }
            .run()
    }

    @Test
    fun `concurrent close는 cached signal로 한 번만 종료한다`() {
        val factory = FakeConnectionFactory("shared")
        val registry = R2dbcConnectionFactoryRegistry.owned(mapOf("primary" to factory))

        MultithreadingTester()
            .workers(8)
            .rounds(4)
            .add { registry.close().block() }
            .run()

        factory.closeCalls.get() shouldBeEqualTo 1
    }

    private open class PlainConnectionFactory(
        private val name: String,
    ) : ConnectionFactory {
        override fun create(): Publisher<out Connection> = Mono.error(UnsupportedOperationException(name))

        override fun getMetadata(): ConnectionFactoryMetadata = object : ConnectionFactoryMetadata {
            override fun getName(): String = name
        }
    }

    private class FakeConnectionFactory(
        name: String,
    ) : PlainConnectionFactory(name), R2dbcCloseable, Disposable {
        val closeCalls = AtomicInteger()
        private val disposed = AtomicInteger()

        override fun close(): Mono<Void> = Mono.fromRunnable {
            closeCalls.incrementAndGet()
        }

        override fun dispose() {
            disposed.incrementAndGet()
        }

        override fun isDisposed(): Boolean = disposed.get() > 0
    }
}
