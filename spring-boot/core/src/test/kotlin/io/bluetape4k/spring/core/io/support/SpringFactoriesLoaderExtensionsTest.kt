package io.bluetape4k.spring.core.io.support

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.support.SpringFactoriesLoader
import java.util.Collections

interface TestFactory {
    val name: String
}

class DefaultFactory : TestFactory {
    override val name = "default"
}

data class RequiredArgument(val value: String)

interface ResolverFactory {
    val requiredArgument: RequiredArgument
}

class RequiredArgumentFactory(override val requiredArgument: RequiredArgument) : ResolverFactory

interface NullableResolverFactory {
    val value: String?
}

class NullableArgumentFactory(override val value: String?) : NullableResolverFactory

interface FailingFactory

class FailingImplementation : FailingFactory {
    init {
        throw IllegalStateException("factory-construction-failed")
    }
}

interface PartialFactory {
    val name: String
}

class AvailablePartialFactory : PartialFactory {
    override val name = "available"
}

class FailingPartialFactory : PartialFactory {
    override val name = "failing"

    init {
        throw IllegalStateException("partial-factory-failed")
    }
}

private const val TEST_RESOURCE_LOCATION = "META-INF/bluetape4k-spring-reified-extension-test.factories"

class SpringFactoriesLoaderExtensionsTest {
    private fun loader() = SpringFactoriesLoader.forResourceLocation(
        TEST_RESOURCE_LOCATION,
        javaClass.classLoader,
    )

    @Test
    fun `loads no argument factory and accepts explicit null strategies`() {
        Collections.list(javaClass.classLoader.getResources(TEST_RESOURCE_LOCATION)).size shouldBeEqualTo 1
        loader().load<TestFactory>().single().name shouldBeEqualTo "default"
        loader().load<TestFactory>(argumentResolver = null, failureHandler = null).single().name shouldBeEqualTo "default"
        loader().load<TestFactory>(argumentResolver = null).single().name shouldBeEqualTo "default"
        loader().load<TestFactory>(failureHandler = null).single().name shouldBeEqualTo "default"
    }

    @Test
    fun `resolves constructor argument types`() {
        val resolvedTypes = mutableListOf<Class<*>>()
        val resolver = SpringFactoriesLoader.ArgumentResolver.from { type ->
            resolvedTypes += type
            if (type == RequiredArgument::class.java) RequiredArgument("resolved") else null
        }
        var failureHandlerCalls = 0
        val failureHandler = SpringFactoriesLoader.FailureHandler { _, _, _ -> failureHandlerCalls++ }

        val factory = loader().load<ResolverFactory>(
            argumentResolver = resolver,
            failureHandler = failureHandler,
        ).single()

        resolvedTypes shouldBeEqualTo listOf(RequiredArgument::class.java)
        factory.requiredArgument.value shouldBeEqualTo "resolved"
        failureHandlerCalls shouldBeEqualTo 0
    }

    @Test
    fun `passes nullable resolver result to Spring constructor`() {
        val resolver = SpringFactoriesLoader.ArgumentResolver.from { type ->
            if (type == String::class.java) null else null
        }

        loader().load<NullableResolverFactory>(argumentResolver = resolver).single().value shouldBeEqualTo null
    }

    @Test
    fun `resolver failure is passed to failure handler`() {
        val resolverFailure = IllegalStateException("resolver-failed")
        var receivedFailure: Throwable? = null
        val resolver = SpringFactoriesLoader.ArgumentResolver.from { throw resolverFailure }
        val handler = SpringFactoriesLoader.FailureHandler { _, _, failure -> receivedFailure = failure }

        loader().load<ResolverFactory>(argumentResolver = resolver, failureHandler = handler).isEmpty().shouldBeTrue()
        generateSequence(receivedFailure) { it.cause }.any { it === resolverFailure }.shouldBeTrue()
    }

    @Test
    fun `failure handler receives the Spring failure context`() {
        var receivedType: Class<*>? = null
        var receivedName: String? = null
        var receivedFailure: Throwable? = null
        val handler = SpringFactoriesLoader.FailureHandler { type, name, failure ->
            receivedType = type
            receivedName = name
            receivedFailure = failure
        }

        loader().load<FailingFactory>(failureHandler = handler).isEmpty().shouldBeTrue()
        receivedType shouldBeEqualTo FailingFactory::class.java
        receivedName shouldBeEqualTo FailingImplementation::class.java.name
        generateSequence(receivedFailure) { it.cause }.any {
            it is IllegalStateException && it.message == "factory-construction-failed"
        }.shouldBeTrue()
    }

    @Test
    fun `non throwing failure handler returns only successfully loaded factories`() {
        var receivedType: Class<*>? = null
        var receivedName: String? = null
        var receivedFailure: Throwable? = null
        val handler = SpringFactoriesLoader.FailureHandler { type, name, failure ->
            receivedType = type
            receivedName = name
            receivedFailure = failure
        }

        val factories = loader().load<PartialFactory>(failureHandler = handler)

        factories.map { it.name } shouldBeEqualTo listOf("available")
        receivedType shouldBeEqualTo PartialFactory::class.java
        receivedName shouldBeEqualTo FailingPartialFactory::class.java.name
        generateSequence(receivedFailure) { it.cause }.any {
            it is IllegalStateException && it.message == "partial-factory-failed"
        }.shouldBeTrue()
    }

    @Test
    fun `default failure handler throws and preserves factory cause`() {
        val failure: Throwable? = assertFailsWith<IllegalArgumentException> {
            loader().load<FailingFactory>()
        }

        generateSequence(failure) { it.cause }.any {
            it is IllegalStateException && it.message == "factory-construction-failed"
        }.shouldBeTrue()
    }
}
