package io.bluetape4k.cache.nearcache.jcache.management

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.nearcache.jcache.NearJCache
import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Test
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.cache.configuration.CompleteConfiguration
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration
import javax.management.MBeanServer
import javax.management.MBeanServerFactory
import javax.management.ObjectName

class NearJCacheMBeanLifecycleTest {

    @Test
    fun `cache close는 등록한 MBean과 front만 정리한다`() {
        val server = MBeanServerFactory.newMBeanServer()
        val fixture = fixture(management = true, statistics = true)
        val registration = fixture.cache.registerMBeans(server, "manager", "cache")
        val names = registration.activeObjectNames

        fixture.cache.close()

        registration.state shouldBeEqualTo NearJCacheMBeanRegistrationState.CLOSED
        names.none(server::isRegistered).shouldBeTrue()
        verify(exactly = 1) { fixture.front.close() }
        verify(exactly = 0) { fixture.back.close() }

        val second = fixture(management = true, statistics = false)
        val serverStillUsable = second.cache.registerMBeans(server, "manager-2", "cache-2")
        serverStillUsable.close()
    }

    @Test
    fun `close가 시작된 뒤 새 MBean registration을 거부한다`() {
        val server = MBeanServerFactory.newMBeanServer()
        val fixture = fixture(management = true, statistics = false)
        fixture.cache.close()

        assertFailsWith<IllegalStateException> {
            fixture.cache.registerMBeans(server, "manager", "cache")
        }

        server.queryNames(ObjectName("io.bluetape4k.cache:*"), null).isEmpty().shouldBeTrue()
    }

    @Test
    fun `unregister 실패 후 다음 cache close는 남은 이름만 재시도한다`() {
        val delegate = MBeanServerFactory.newMBeanServer()
        val unregisterCalls = CopyOnWriteArrayList<ObjectName>()
        val failureCount = AtomicInteger()
        val server = proxyServer(delegate) { methodName, arguments, invokeDelegate ->
            if (methodName == "unregisterMBean") {
                val name = arguments!![0] as ObjectName
                unregisterCalls += name
                if (failureCount.getAndIncrement() == 0) throw IllegalStateException("unregister failed")
            }
            invokeDelegate(arguments)
        }
        val fixture = fixture(management = true, statistics = true)
        val registration = fixture.cache.registerMBeans(server, "manager", "cache")
        val statisticsName = registration.activeObjectNames.single {
            it.getKeyProperty("type") == "NearJCacheStatistics"
        }
        val configurationName = registration.activeObjectNames.single {
            it.getKeyProperty("type") == "NearJCacheConfiguration"
        }

        assertFailsWith<NearJCacheMBeanRegistrationException> { fixture.cache.close() }
        registration.state shouldBeEqualTo NearJCacheMBeanRegistrationState.RECOVERY_REQUIRED
        registration.activeObjectNames shouldBeEqualTo setOf(statisticsName)

        fixture.cache.close()

        registration.state shouldBeEqualTo NearJCacheMBeanRegistrationState.CLOSED
        unregisterCalls.count { it == statisticsName } shouldBeEqualTo 2
        unregisterCalls.count { it == configurationName } shouldBeEqualTo 1
        verify(exactly = 1) { fixture.front.close() }
        verify(exactly = 0) { fixture.back.close() }
    }

    @Test
    fun `cache close는 JMX listener front 실패를 resource 순서대로 보존한다`() {
        val delegate = MBeanServerFactory.newMBeanServer()
        val jmxFailure = IllegalStateException("jmx cleanup failed")
        val listenerFailure = IllegalArgumentException("listener cleanup failed")
        val frontFailure = UnsupportedOperationException("front cleanup failed")
        val server = proxyServer(delegate) { methodName, arguments, invokeDelegate ->
            if (methodName == "unregisterMBean") throw jmxFailure
            invokeDelegate(arguments)
        }
        val fixture = fixture(management = true, statistics = false)
        every { fixture.back.registerCacheEntryListener(any()) } returns Unit
        every { fixture.back.deregisterCacheEntryListener(any()) } throws listenerFailure
        every { fixture.front.close() } throws frontFailure
        fixture.cache.registerBackCacheListener()
        fixture.cache.registerMBeans(server, "manager", "cache")

        val failure = assertFailsWith<NearJCacheMBeanRegistrationException> { fixture.cache.close() }

        failure.cause shouldBeEqualTo jmxFailure
        failure.suppressed.toList() shouldBeEqualTo listOf(listenerFailure, frontFailure)
        verify(exactly = 1) { fixture.back.deregisterCacheEntryListener(any()) }
        verify(exactly = 1) { fixture.front.close() }
    }

    @Test
    fun `registration reservation 뒤 시작한 close는 publish와 cleanup을 기다린다`() {
        val enteredRegister = CountDownLatch(1)
        val releaseRegister = CountDownLatch(1)
        val delegate = MBeanServerFactory.newMBeanServer()
        val server = proxyServer(delegate) { methodName, arguments, invokeDelegate ->
            if (methodName == "registerMBean") {
                enteredRegister.countDown()
                releaseRegister.await(5, TimeUnit.SECONDS).shouldBeTrue()
            }
            invokeDelegate(arguments)
        }
        val fixture = fixture(management = true, statistics = false)
        val executor = Executors.newFixedThreadPool(2)
        try {
            assertTimeoutPreemptively(Duration.ofSeconds(5)) {
                val registrationFuture = executor.submit<NearJCacheMBeanRegistration> {
                    fixture.cache.registerMBeans(server, "manager", "cache")
                }
                enteredRegister.await(2, TimeUnit.SECONDS).shouldBeTrue()
                val closeFuture = executor.submit { fixture.cache.close() }

                closeFuture.isDone.shouldBeFalse()
                releaseRegister.countDown()

                val registration = registrationFuture.get(2, TimeUnit.SECONDS)
                closeFuture.get(2, TimeUnit.SECONDS)
                registration.state shouldBeEqualTo NearJCacheMBeanRegistrationState.CLOSED
                registration.activeObjectNames.isEmpty().shouldBeTrue()
            }
        } finally {
            releaseRegister.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `explicit handle close와 cache close는 같은 cleanup attempt를 기다린다`() {
        val enteredUnregister = CountDownLatch(1)
        val releaseUnregister = CountDownLatch(1)
        val unregisterCalls = AtomicInteger()
        val delegate = MBeanServerFactory.newMBeanServer()
        val server = proxyServer(delegate) { methodName, arguments, invokeDelegate ->
            if (methodName == "unregisterMBean") {
                unregisterCalls.incrementAndGet()
                enteredUnregister.countDown()
                releaseUnregister.await(5, TimeUnit.SECONDS).shouldBeTrue()
            }
            invokeDelegate(arguments)
        }
        val fixture = fixture(management = true, statistics = false)
        val registration = fixture.cache.registerMBeans(server, "manager", "cache")
        val frontClosed = CountDownLatch(1)
        every { fixture.front.close() } answers { frontClosed.countDown() }
        val executor = Executors.newFixedThreadPool(2)
        try {
            assertTimeoutPreemptively(Duration.ofSeconds(5)) {
                val handleClose = executor.submit { registration.close() }
                enteredUnregister.await(2, TimeUnit.SECONDS).shouldBeTrue()
                val cacheClose = executor.submit { fixture.cache.close() }

                cacheClose.isDone.shouldBeFalse()
                frontClosed.await(100, TimeUnit.MILLISECONDS).shouldBeFalse()
                releaseUnregister.countDown()

                handleClose.get(2, TimeUnit.SECONDS)
                cacheClose.get(2, TimeUnit.SECONDS)
                unregisterCalls.get() shouldBeEqualTo 1
                registration.state shouldBeEqualTo NearJCacheMBeanRegistrationState.CLOSED
                frontClosed.await(2, TimeUnit.SECONDS).shouldBeTrue()
            }
        } finally {
            releaseUnregister.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `동시 handle close caller는 같은 실패 snapshot을 공유한다`() {
        val enteredUnregister = CountDownLatch(1)
        val releaseUnregister = CountDownLatch(1)
        val failOnce = AtomicBoolean(true)
        val unregisterCalls = AtomicInteger()
        val delegate = MBeanServerFactory.newMBeanServer()
        val server = proxyServer(delegate) { methodName, arguments, invokeDelegate ->
            if (methodName == "unregisterMBean") {
                unregisterCalls.incrementAndGet()
                enteredUnregister.countDown()
                releaseUnregister.await(5, TimeUnit.SECONDS).shouldBeTrue()
                if (failOnce.compareAndSet(true, false)) throw IllegalStateException("unregister failed")
            }
            invokeDelegate(arguments)
        }
        val fixture = fixture(management = true, statistics = false)
        val registration = fixture.cache.registerMBeans(server, "manager", "cache")
        val executor = Executors.newFixedThreadPool(2)
        try {
            assertTimeoutPreemptively(Duration.ofSeconds(5)) {
                val first = executor.submit { registration.close() }
                enteredUnregister.await(2, TimeUnit.SECONDS).shouldBeTrue()
                val secondStarted = CountDownLatch(1)
                val second = executor.submit {
                    secondStarted.countDown()
                    registration.close()
                }
                secondStarted.await(2, TimeUnit.SECONDS).shouldBeTrue()
                releaseUnregister.countDown()

                val firstFailure = assertFailsWith<ExecutionException> {
                    first.get(2, TimeUnit.SECONDS)
                }.cause
                val secondFailure = assertFailsWith<ExecutionException> {
                    second.get(2, TimeUnit.SECONDS)
                }.cause

                (firstFailure === secondFailure).shouldBeTrue()
                unregisterCalls.get() shouldBeEqualTo 1
                registration.state shouldBeEqualTo NearJCacheMBeanRegistrationState.RECOVERY_REQUIRED
            }
            registration.close()
            registration.state shouldBeEqualTo NearJCacheMBeanRegistrationState.CLOSED
        } finally {
            releaseUnregister.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `MBeanServer callback의 same-thread cache close 재진입은 fail-fast한다`() {
        val callbackFailure = AtomicReference<Throwable?>()
        val delegate = MBeanServerFactory.newMBeanServer()
        lateinit var fixture: Fixture
        val server = proxyServer(delegate) { methodName, arguments, invokeDelegate ->
            if (methodName == "registerMBean") {
                callbackFailure.set(runCatching { fixture.cache.close() }.exceptionOrNull())
            }
            invokeDelegate(arguments)
        }
        fixture = fixture(management = true, statistics = false)

        val registration = fixture.cache.registerMBeans(server, "manager", "cache")

        (callbackFailure.get() is IllegalStateException).shouldBeTrue()
        fixture.front.isClosed.shouldBeFalse()
        registration.close()
    }

    @Test
    fun `MBeanServer callback의 same-thread registration 재진입은 fail-fast한다`() {
        val callbackFailure = AtomicReference<Throwable?>()
        val callbackOnce = AtomicBoolean(true)
        val delegate = MBeanServerFactory.newMBeanServer()
        lateinit var fixture: Fixture
        lateinit var server: MBeanServer
        server = proxyServer(delegate) { methodName, arguments, invokeDelegate ->
            if (methodName == "registerMBean" && callbackOnce.compareAndSet(true, false)) {
                callbackFailure.set(
                    runCatching {
                        fixture.cache.registerMBeans(server, "nested-manager", "nested-cache")
                    }.exceptionOrNull()
                )
            }
            invokeDelegate(arguments)
        }
        fixture = fixture(management = true, statistics = false)

        val registration = fixture.cache.registerMBeans(server, "manager", "cache")

        (callbackFailure.get() is IllegalStateException).shouldBeTrue()
        registration.state shouldBeEqualTo NearJCacheMBeanRegistrationState.REGISTERED
        registration.close()
    }

    @Test
    fun `registration rollback recovery handle은 이후 cache close가 정리한다`() {
        val delegate = MBeanServerFactory.newMBeanServer()
        val registerCalls = AtomicInteger()
        val failRollbackOnce = AtomicBoolean(true)
        val server = proxyServer(delegate) { methodName, arguments, invokeDelegate ->
            when {
                methodName == "registerMBean" && registerCalls.incrementAndGet() == 2 ->
                    throw IllegalStateException("second registration failed")

                methodName == "unregisterMBean" && failRollbackOnce.compareAndSet(true, false) ->
                    throw IllegalArgumentException("rollback failed")

                else -> invokeDelegate(arguments)
            }
        }
        val fixture = fixture(management = true, statistics = true)
        val failure = assertFailsWith<NearJCacheMBeanRegistrationException> {
            fixture.cache.registerMBeans(server, "manager", "cache")
        }
        val recovery = failure.recoveryRegistration!!

        fixture.cache.close()

        recovery.state shouldBeEqualTo NearJCacheMBeanRegistrationState.CLOSED
        recovery.activeObjectNames.isEmpty().shouldBeTrue()
        verify(exactly = 1) { fixture.front.close() }
    }

    private fun fixture(management: Boolean, statistics: Boolean): Fixture {
        val configuration = MutableConfiguration<String, String>()
            .setTypes(String::class.java, String::class.java)
            .setStoreByValue(false)
            .setManagementEnabled(management)
            .setStatisticsEnabled(statistics)
        val front = configuredCache(configuration)
        val back = configuredCache(configuration)
        return Fixture(
            cache = NearJCache(
                frontCache = front,
                backCache = back,
                config = NearJCacheConfig(frontCacheConfiguration = configuration),
            ),
            front = front,
            back = back,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun configuredCache(configuration: MutableConfiguration<String, String>): JCache<String, String> {
        val cache = mockk<JCache<String, String>>(relaxed = true)
        val configurationClass = Configuration::class.java as Class<Configuration<String, String>>
        val completeConfigurationClass =
            CompleteConfiguration::class.java as Class<CompleteConfiguration<String, String>>
        every { cache.getConfiguration(configurationClass) } returns configuration
        every { cache.getConfiguration(completeConfigurationClass) } returns configuration
        return cache
    }

    private fun proxyServer(
        delegate: MBeanServer,
        interceptor: (String, Array<out Any?>?, (Array<out Any?>?) -> Any?) -> Any?,
    ): MBeanServer = Proxy.newProxyInstance(
        MBeanServer::class.java.classLoader,
        arrayOf(MBeanServer::class.java),
    ) { _, method, arguments ->
        interceptor(method.name, arguments) { forwarded ->
            try {
                method.invoke(delegate, *(forwarded ?: emptyArray()))
            } catch (e: InvocationTargetException) {
                throw e.targetException
            }
        }
    } as MBeanServer

    private data class Fixture(
        val cache: NearJCache<String, String>,
        val front: JCache<String, String>,
        val back: JCache<String, String>,
    )
}
