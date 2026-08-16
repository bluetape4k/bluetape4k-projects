package io.bluetape4k.cache.nearcache.jcache.management

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.nearcache.jcache.NearJCache
import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.cache.configuration.CompleteConfiguration
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration
import javax.management.InstanceAlreadyExistsException
import javax.management.MBeanServer
import javax.management.MBeanServerFactory
import javax.management.ObjectName
import javax.management.StandardMBean

class NearJCacheMBeanRegistrationTest {

    @Test
    fun `management와 statistics flag 조합대로 bean을 등록한다`() {
        listOf(
            Triple(true, false, setOf("NearJCacheConfiguration")),
            Triple(false, true, setOf("NearJCacheStatistics")),
            Triple(true, true, setOf("NearJCacheConfiguration", "NearJCacheStatistics")),
        ).forEach { (management, statistics, expectedTypes) ->
            val server = MBeanServerFactory.newMBeanServer()
            val fixture = fixture(management, statistics)

            val registration = fixture.cache.registerMBeans(server, "manager", "cache")

            registration.activeObjectNames.mapTo(linkedSetOf()) {
                it.getKeyProperty("type")
            } shouldBeEqualTo expectedTypes
            registration.activeObjectNames.all(server::isRegistered).shouldBeTrue()
        }
    }

    @Test
    fun `두 flag가 모두 꺼져 있으면 server mutation 전에 거부한다`() {
        val server = countingServer()
        val fixture = fixture(management = false, statistics = false)

        assertFailsWith<IllegalStateException> {
            fixture.cache.registerMBeans(server.server, "manager", "cache")
        }

        server.registerCalls.get() shouldBeEqualTo 0
    }

    @Test
    fun `ID는 원문을 보존하고 특수 문자를 quote한 non-pattern ObjectName을 만든다`() {
        val managerId = "Manager:한글,= *?\\\""
        val cacheId = "Cache:e\u0301,= *?\\\""
        val name = registeredStatisticsName(managerId, cacheId)

        ObjectName.unquote(name.getKeyProperty("manager")) shouldBeEqualTo managerId
        ObjectName.unquote(name.getKeyProperty("cache")) shouldBeEqualTo cacheId
        name.isPattern.shouldBeFalse()
        name.isDomainPattern.shouldBeFalse()
        name.isPropertyPattern.shouldBeFalse()
        registeredStatisticsName("manager", "cache") shouldBeEqualTo
                registeredStatisticsName("manager", "cache")
        (registeredStatisticsName("Manager", "cache") !=
                registeredStatisticsName("manager", "cache")).shouldBeTrue()
        (registeredStatisticsName("\u00e9", "cache") !=
                registeredStatisticsName("e\u0301", "cache")).shouldBeTrue()
    }

    @Test
    fun `blank 길이 초과 control 문자와 앞뒤 whitespace ID를 거부한다`() {
        listOf("", " ", " manager", "manager ", "a\nb", "a\u0000b", "a".repeat(257)).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                fixture(management = false, statistics = true).cache.registerMBeans(
                    MBeanServerFactory.newMBeanServer(),
                    invalid,
                    "cache",
                )
            }
            assertFailsWith<IllegalArgumentException> {
                fixture(management = false, statistics = true).cache.registerMBeans(
                    MBeanServerFactory.newMBeanServer(),
                    "manager",
                    invalid,
                )
            }
        }
    }

    @Test
    fun `statistics ObjectName 하나가 표준과 tier 속성 및 clear만 노출한다`() {
        val server = MBeanServerFactory.newMBeanServer()
        val fixture = fixture(management = true, statistics = true)
        val registration = fixture.cache.registerMBeans(server, "manager", "cache")
        val statisticsName = registration.activeObjectNames.single {
            it.getKeyProperty("type") == "NearJCacheStatistics"
        }

        val info = server.getMBeanInfo(statisticsName)
        val attributeNames = info.attributes.mapTo(mutableSetOf()) { it.name }
        val operationNames = info.operations.mapTo(mutableSetOf()) { it.name }

        ("CacheHits" in attributeNames).shouldBeTrue()
        ("FrontHits" in attributeNames).shouldBeTrue()
        ("SupportedOperations" in attributeNames).shouldBeTrue()
        operationNames shouldBeEqualTo setOf("clear")
        info.descriptor.getFieldValue("nearJCacheRegistrationToken").toString().isNotBlank().shouldBeTrue()
    }

    @Test
    fun `두 번째 이름 collision은 첫 owned MBean만 rollback한다`() {
        val server = MBeanServerFactory.newMBeanServer()
        val fixture = fixture(management = true, statistics = true)
        val configurationName = expectedObjectName("NearJCacheConfiguration", "manager", "cache")
        val statisticsName = expectedObjectName("NearJCacheStatistics", "manager", "cache")
        server.registerMBean(foreignStatisticsBean(), statisticsName)

        assertFailsWith<InstanceAlreadyExistsException> {
            fixture.cache.registerMBeans(server, "manager", "cache")
        }

        server.isRegistered(statisticsName).shouldBeTrue()
        server.isRegistered(configurationName).shouldBeFalse()
    }

    @Test
    fun `rollback 실패는 suppressed와 recovery handle에 남긴다`() {
        val delegate = MBeanServerFactory.newMBeanServer()
        val registerCount = AtomicInteger()
        val failUnregisterOnce = AtomicBoolean(true)
        val server = proxyServer(delegate) { methodName, arguments, invokeDelegate ->
            when {
                methodName == "registerMBean" && registerCount.incrementAndGet() == 2 ->
                    throw InstanceAlreadyExistsException("second registration failed")

                methodName == "unregisterMBean" && failUnregisterOnce.compareAndSet(true, false) ->
                    throw IllegalStateException("rollback failed")

                else -> invokeDelegate(arguments)
            }
        }
        val fixture = fixture(management = true, statistics = true)

        val failure = assertFailsWith<NearJCacheMBeanRegistrationException> {
            fixture.cache.registerMBeans(server, "manager", "cache")
        }

        (failure.cause is InstanceAlreadyExistsException).shouldBeTrue()
        failure.cause!!.suppressed.single().message shouldBeEqualTo "rollback failed"
        failure.remainingObjectNames.size shouldBeEqualTo 1
        failure.recoveryRegistration!!.activeObjectNames shouldBeEqualTo failure.remainingObjectNames

        failure.recoveryRegistration.close()
        failure.recoveryRegistration.state shouldBeEqualTo NearJCacheMBeanRegistrationState.CLOSED
    }

    @Test
    fun `foreign replacement는 보존하고 handle을 recovery required로 유지한다`() {
        val server = MBeanServerFactory.newMBeanServer()
        val fixture = fixture(management = true, statistics = true)
        val registration = fixture.cache.registerMBeans(server, "manager", "cache")
        val statisticsName = registration.activeObjectNames.single {
            it.getKeyProperty("type") == "NearJCacheStatistics"
        }
        server.unregisterMBean(statisticsName)
        server.registerMBean(foreignStatisticsBean(), statisticsName)

        val failure = assertFailsWith<NearJCacheMBeanRegistrationException> { registration.close() }

        failure.remainingObjectNames shouldBeEqualTo setOf(statisticsName)
        registration.state shouldBeEqualTo NearJCacheMBeanRegistrationState.RECOVERY_REQUIRED
        registration.activeObjectNames shouldBeEqualTo setOf(statisticsName)
        server.isRegistered(statisticsName).shouldBeTrue()

        server.unregisterMBean(statisticsName)
        registration.close()
        registration.state shouldBeEqualTo NearJCacheMBeanRegistrationState.CLOSED
    }

    @Test
    fun `active ObjectName은 Java caller도 변경할 수 없는 snapshot이다`() {
        val server = MBeanServerFactory.newMBeanServer()
        val fixture = fixture(management = true, statistics = false)
        val registration = fixture.cache.registerMBeans(server, "manager", "cache")
        @Suppress("UNCHECKED_CAST")
        val snapshot = registration.activeObjectNames as MutableSet<ObjectName>

        assertFailsWith<UnsupportedOperationException> { snapshot.clear() }
        registration.activeObjectNames.size shouldBeEqualTo 1

        registration.close()
        registration.isClosed.shouldBeTrue()
        registration.activeObjectNames.isEmpty().shouldBeTrue()
        registration.close()
    }

    @Test
    fun `이미 사라진 owned MBean은 close 성공으로 처리한다`() {
        val server = MBeanServerFactory.newMBeanServer()
        val fixture = fixture(management = true, statistics = false)
        val registration = fixture.cache.registerMBeans(server, "manager", "cache")
        server.unregisterMBean(registration.activeObjectNames.single())

        registration.close()

        registration.state shouldBeEqualTo NearJCacheMBeanRegistrationState.CLOSED
        registration.activeObjectNames.isEmpty().shouldBeTrue()
    }

    @Test
    fun `registration exception 직렬화는 remaining names만 보존한다`() {
        val server = MBeanServerFactory.newMBeanServer()
        val fixture = fixture(management = true, statistics = false)
        val registration = fixture.cache.registerMBeans(server, "manager", "cache")
        val failure = NearJCacheMBeanRegistrationException(
            recoveryRegistration = registration,
            remainingObjectNames = registration.activeObjectNames,
            cause = IllegalStateException("cleanup failed"),
        )

        val serialized = ByteArrayOutputStream().use { bytes ->
            ObjectOutputStream(bytes).use { it.writeObject(failure) }
            bytes.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(serialized)).use {
            it.readObject() as NearJCacheMBeanRegistrationException
        }

        restored.recoveryRegistration.shouldBeNull()
        restored.remainingObjectNames shouldBeEqualTo failure.remainingObjectNames
        @Suppress("UNCHECKED_CAST")
        val snapshot = restored.remainingObjectNames as MutableSet<ObjectName>
        assertFailsWith<UnsupportedOperationException> { snapshot.clear() }
    }

    @Test
    fun `Java facade는 default overload 없이 exact static descriptor를 제공한다`() {
        val methods = Class.forName("io.bluetape4k.cache.nearcache.jcache.management.NearJCacheMBeans")
            .declaredMethods
            .filter { it.name == "registerMBeans" }

        methods.size shouldBeEqualTo 1
        methods.single().parameterTypes.toList() shouldBeEqualTo listOf(
            NearJCache::class.java,
            MBeanServer::class.java,
            String::class.java,
            String::class.java,
        )
        methods.single().returnType shouldBeEqualTo NearJCacheMBeanRegistration::class.java
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
        )
    }

    private fun registeredStatisticsName(managerId: String, cacheId: String): ObjectName {
        val server = MBeanServerFactory.newMBeanServer()
        val registration = fixture(management = false, statistics = true)
            .cache
            .registerMBeans(server, managerId, cacheId)
        return registration.activeObjectNames.single().also { registration.close() }
    }

    private fun expectedObjectName(type: String, managerId: String, cacheId: String): ObjectName = ObjectName(
        "io.bluetape4k.cache:type=$type," +
                "manager=${ObjectName.quote(managerId)},cache=${ObjectName.quote(cacheId)}"
    )

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

    private fun foreignStatisticsBean(): StandardMBean = StandardMBean(
        EmptyNearJCacheStatisticsMXBean(),
        NearJCacheTierStatisticsMXBean::class.java,
        true,
    )

    private fun countingServer(): CountingServer {
        val delegate = MBeanServerFactory.newMBeanServer()
        val registerCalls = AtomicInteger()
        return CountingServer(
            server = proxyServer(delegate) { methodName, arguments, invokeDelegate ->
                if (methodName == "registerMBean") registerCalls.incrementAndGet()
                invokeDelegate(arguments)
            },
            registerCalls = registerCalls,
        )
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
    )

    private data class CountingServer(
        val server: MBeanServer,
        val registerCalls: AtomicInteger,
    )
}
