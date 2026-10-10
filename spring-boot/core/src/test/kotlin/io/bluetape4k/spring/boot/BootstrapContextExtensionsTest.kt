package io.bluetape4k.spring.boot

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.bootstrap.BootstrapRegistry
import org.springframework.boot.bootstrap.DefaultBootstrapContext
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

class BootstrapContextExtensionsTest {
    class SharedResource
    class MissingResource
    class NullableResource
    class MissingException : RuntimeException()

    private val shared = SharedResource()
    private val fallback = MissingResource()
    private val lazyFallback = MissingResource()
    private val missingException = MissingException()

    private fun registeredContext(): DefaultBootstrapContext = DefaultBootstrapContext().apply {
        register(SharedResource::class.java, BootstrapRegistry.InstanceSupplier { shared })
        register(NullableResource::class.java, BootstrapRegistry.InstanceSupplier { null })
    }

    @Test
    fun `registered and missing lookups preserve Spring results`() {
        val context = registeredContext()

        context.get<SharedResource>() shouldBeSameInstanceAs shared
        context.isRegistered<SharedResource>().shouldBeTrue()
        context.isRegistered<MissingResource>().shouldBeFalse()
        assertFailsWith<IllegalStateException> { context.get<MissingResource>() }
        context.get<NullableResource>().shouldBeNull()
        context.getOrElse<NullableResource>(NullableResource()).shouldBeNull()
    }

    @Test
    fun `defaults and suppliers are used only for missing types`() {
        val context = registeredContext()
        val supplierCalls = AtomicInteger()
        val registeredSupplierCalls = AtomicInteger()
        val supplier = Supplier<MissingResource?> {
            supplierCalls.incrementAndGet()
            lazyFallback
        }
        val registeredSupplier = Supplier<SharedResource?> {
            registeredSupplierCalls.incrementAndGet()
            null
        }

        context.getOrElse<SharedResource>(null) shouldBeSameInstanceAs shared
        context.getOrElseSupply<SharedResource>(registeredSupplier) shouldBeSameInstanceAs shared
        registeredSupplierCalls.get() shouldBeEqualTo 0
        context.getOrElse<MissingResource>(fallback) shouldBeSameInstanceAs fallback
        context.getOrElseSupply<MissingResource>(supplier) shouldBeSameInstanceAs lazyFallback
        supplierCalls.get() shouldBeEqualTo 1
    }

    @Test
    fun `exception supplier runs only for missing types`() {
        val context = registeredContext()
        val supplierCalls = AtomicInteger()
        val supplier = Supplier<Throwable> {
            supplierCalls.incrementAndGet()
            missingException
        }

        context.getOrElseThrow<SharedResource>(supplier) shouldBeSameInstanceAs shared
        supplierCalls.get() shouldBeEqualTo 0
        val actual = assertFailsWith<MissingException> {
            context.getOrElseThrow<MissingResource>(supplier)
        }
        actual shouldBeSameInstanceAs missingException
        supplierCalls.get() shouldBeEqualTo 1
    }
}
