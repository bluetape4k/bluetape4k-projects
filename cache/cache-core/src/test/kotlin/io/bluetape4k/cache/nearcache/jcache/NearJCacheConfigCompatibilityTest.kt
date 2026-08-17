package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.cache.nearcache.jcache.management.NearJCacheManagementMXBean
import io.bluetape4k.cache.nearcache.jcache.management.NearJCacheConfigurationMXBean
import io.bluetape4k.cache.nearcache.jcache.management.NearJCacheStatisticsMXBean
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InvalidObjectException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass
import java.io.ObjectStreamConstants
import java.io.Serializable
import javax.cache.Cache
import javax.cache.CacheManager
import javax.cache.configuration.Factory
import javax.cache.configuration.MutableConfiguration

class NearJCacheConfigCompatibilityTest {

    @Test
    fun `NearJCache와 management bean의 public constructor ABI를 유지한다`() {
        val publicConstructors = NearJCache::class.java.constructors.filterNot { it.isSynthetic }
        publicConstructors.size shouldBeEqualTo 2
        publicConstructors.map { it.parameterTypes.toList() }.toSet() shouldBeEqualTo setOf(
            listOf(Cache::class.java, Cache::class.java, NearJCacheConfig::class.java),
            listOf(
                Cache::class.java,
                Cache::class.java,
                NearJCacheConfig::class.java,
                NearJCacheClearAuthority::class.java,
            ),
        )
        NearJCacheManagementMXBean::class.java.getConstructor(NearJCache::class.java).shouldNotBeNull()
        NearJCacheConfigurationMXBean::class.java
            .getMethod("getBulkFrontPopulationPolicy")
            .returnType shouldBeEqualTo String::class.java
        NearJCacheConfigurationMXBean::class.java
            .getMethod("getBulkFrontPopulationMaximumEntryCount")
            .returnType shouldBeEqualTo Int::class.javaPrimitiveType
        NearJCacheManagementMXBean::class.java
            .getMethod("getBulkFrontPopulationPolicy")
            .returnType shouldBeEqualTo String::class.java
        NearJCacheManagementMXBean::class.java
            .getMethod("getBulkFrontPopulationMaximumEntryCount")
            .returnType shouldBeEqualTo Int::class.javaPrimitiveType
        NearJCacheStatisticsMXBean::class.java.getConstructor().shouldNotBeNull()
        NearJCacheStatisticsMXBean::class.java
            .getMethod("addRemovals", Long::class.javaPrimitiveType)
            .returnType shouldBeEqualTo Void.TYPE
    }

    @Test
    fun `기존 Java MXBean 구현체는 새 authority getter의 DENY 기본값을 사용한다`() {
        LegacyNearJCacheConfigurationMXBean().getClearAuthority() shouldBeEqualTo "DENY"
    }

    @Test
    fun `NearJCacheConfig data class ABI를 유지한다`() {
        verifyDirectConstructors()
        verifySyntheticConstructors()
        verifyDirectCopyMethods()
        verifyCopyDefaultMethods()
        verifyComponentsAndSerialVersion()
    }

    private fun verifyDirectConstructors() {
        val configClass = NearJCacheConfig::class.java
        val factoryType = Factory::class.java
        val configurationType = MutableConfiguration::class.java

        configClass.getConstructor(
            factoryType,
            String::class.java,
            configurationType,
            Boolean::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
        ).shouldNotBeNull()
        configClass.getConstructor(
            factoryType,
            String::class.java,
            configurationType,
            Boolean::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        ).shouldNotBeNull()
        configClass.getConstructor(
            factoryType,
            String::class.java,
            configurationType,
            Boolean::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            BulkFrontPopulationPolicy::class.java,
        ).shouldNotBeNull()
        configClass.getConstructor().shouldNotBeNull()
    }

    private fun verifySyntheticConstructors() {
        val configClass = NearJCacheConfig::class.java
        val factoryType = Factory::class.java
        val configurationType = MutableConfiguration::class.java
        val markerType = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker")
        configClass.getDeclaredConstructor(
            factoryType,
            String::class.java,
            configurationType,
            Boolean::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            markerType,
        ).shouldNotBeNull()
        configClass.getDeclaredConstructor(
            factoryType,
            String::class.java,
            configurationType,
            Boolean::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            markerType,
        ).shouldNotBeNull()
        configClass.getDeclaredConstructor(
            factoryType,
            String::class.java,
            configurationType,
            Boolean::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            BulkFrontPopulationPolicy::class.java,
            Int::class.javaPrimitiveType,
            markerType,
        ).shouldNotBeNull()
    }

    private fun verifyDirectCopyMethods() {
        val configClass = NearJCacheConfig::class.java
        val factoryType = Factory::class.java
        val configurationType = MutableConfiguration::class.java
        val copy = configClass.getMethod(
            "copy",
            factoryType,
            String::class.java,
            configurationType,
            Boolean::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        )
        copy.returnType shouldBeEqualTo configClass
        configClass.getMethod(
            "copy",
            factoryType,
            String::class.java,
            configurationType,
            Boolean::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
        ).returnType shouldBeEqualTo configClass
        val copyCurrent = configClass.getMethod(
            "copy",
            factoryType,
            String::class.java,
            configurationType,
            Boolean::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            BulkFrontPopulationPolicy::class.java,
        )
        copyCurrent.returnType shouldBeEqualTo configClass
    }

    private fun verifyCopyDefaultMethods() {
        val configClass = NearJCacheConfig::class.java
        val factoryType = Factory::class.java
        val configurationType = MutableConfiguration::class.java
        configClass.getDeclaredMethod(
            "copy\$default",
            configClass,
            factoryType,
            String::class.java,
            configurationType,
            Boolean::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Any::class.java,
        ).returnType shouldBeEqualTo configClass
        configClass.getDeclaredMethod(
            "copy\$default",
            configClass,
            factoryType,
            String::class.java,
            configurationType,
            Boolean::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            BulkFrontPopulationPolicy::class.java,
            Int::class.javaPrimitiveType,
            Any::class.java,
        ).returnType shouldBeEqualTo configClass
        configClass.getDeclaredMethod(
            "copy\$default",
            configClass,
            factoryType,
            String::class.java,
            configurationType,
            Boolean::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Any::class.java,
        ).returnType shouldBeEqualTo configClass
    }

    private fun verifyComponentsAndSerialVersion() {
        val configClass = NearJCacheConfig::class.java
        (1..7).forEach { component ->
            configClass.getMethod("component$component").shouldNotBeNull()
        }
        configClass.getMethod("component7").returnType shouldBeEqualTo BulkFrontPopulationPolicy::class.java
        ObjectStreamClass.lookup(configClass).serialVersionUID shouldBeEqualTo 1L
    }

    @Test
    fun `prior five argument Java consumer ABI remains callable`() {
        val factory = SerializableCacheManagerFactory()
        val frontConfiguration = MutableConfiguration<String, String>()
        val configClass = NearJCacheConfig::class.java

        configClass.getMethod("getSyncRemoteRetryCount").returnType shouldBeEqualTo Int::class.javaPrimitiveType
        configClass.getMethod("component6").returnType shouldBeEqualTo Int::class.javaPrimitiveType

        val config = LegacyNearJCacheConfigConsumer.construct(
            factory,
            "legacy-cache",
            frontConfiguration,
            true,
            250L,
        )
        val copy = LegacyNearJCacheConfigConsumer.copy(
            config,
            factory,
            "legacy-copy",
            frontConfiguration,
            false,
            500L,
        )
        val kotlinConfig = NearJCacheConfig<String, String>(
            factory,
            "kotlin-legacy-cache",
            frontConfiguration,
            true,
            250L,
        )
        val kotlinCopy = kotlinConfig.copy(
            factory,
            "kotlin-legacy-copy",
            frontConfiguration,
            false,
            500L,
        )

        config.cacheName shouldBeEqualTo "legacy-cache"
        config.syncRemoteRetryCount shouldBeEqualTo NearJCacheConfig.DEFAULT_SYNC_REMOTE_RETRY_COUNT
        config.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
        copy.cacheName shouldBeEqualTo "legacy-copy"
        copy.syncRemoteRetryCount shouldBeEqualTo NearJCacheConfig.DEFAULT_SYNC_REMOTE_RETRY_COUNT
        copy.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
        kotlinConfig.cacheName shouldBeEqualTo "kotlin-legacy-cache"
        kotlinCopy.cacheName shouldBeEqualTo "kotlin-legacy-copy"
        kotlinConfig.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
        kotlinCopy.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
    }

    @Test
    fun `current Java consumer invokes no-arg and six and seven argument ABI`() {
        val factory = SerializableCacheManagerFactory()
        val frontConfiguration = MutableConfiguration<String, String>()
        val bounded = BulkFrontPopulationPolicy.PopulateIfAtMost(2)
        val defaultConfig = CurrentNearJCacheConfigConsumer.constructWithNoArguments()
        val sixArgumentConfig = CurrentNearJCacheConfigConsumer.constructWithSixArguments(
            factory,
            "current-six",
            frontConfiguration,
            false,
            250L,
            2,
        )
        val sevenArgumentConfig = CurrentNearJCacheConfigConsumer.constructWithSevenArguments(
            factory,
            "current-seven",
            frontConfiguration,
            false,
            250L,
            2,
            bounded,
        )
        val sixArgumentCopy = CurrentNearJCacheConfigConsumer.copyWithSixArguments(
            sevenArgumentConfig,
            factory,
            "current-six-copy",
            frontConfiguration,
            true,
            500L,
            3,
        )
        val sevenArgumentCopy = CurrentNearJCacheConfigConsumer.copyWithSevenArguments(
            sixArgumentConfig,
            factory,
            "current-seven-copy",
            frontConfiguration,
            true,
            500L,
            3,
            bounded,
        )

        defaultConfig.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
        sixArgumentConfig.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
        CurrentNearJCacheConfigConsumer.policy(sevenArgumentConfig) shouldBeEqualTo bounded
        sixArgumentCopy.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
        CurrentNearJCacheConfigConsumer.policy(sevenArgumentCopy) shouldBeEqualTo bounded
    }

    @Test
    fun `old copy default bridges intentionally reset a bounded policy`() {
        val source = NearJCacheConfig<String, String>(
            bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(2),
        )
        val configClass = NearJCacheConfig::class.java
        val factoryType = Factory::class.java
        val configurationType = MutableConfiguration::class.java
        val copyDefault5 = configClass.getDeclaredMethod(
            "copy\$default",
            configClass,
            factoryType,
            String::class.java,
            configurationType,
            Boolean::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Any::class.java,
        )
        val copyDefault6 = configClass.getDeclaredMethod(
            "copy\$default",
            configClass,
            factoryType,
            String::class.java,
            configurationType,
            Boolean::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Any::class.java,
        )

        val copy5 = copyDefault5.invoke(
            null,
            source,
            null,
            null,
            null,
            false,
            0L,
            FIVE_FIELD_DEFAULT_MASK,
            null,
        ) as NearJCacheConfig<*, *>
        val copy6 = copyDefault6.invoke(
            null,
            source,
            null,
            null,
            null,
            false,
            0L,
            0,
            SIX_FIELD_DEFAULT_MASK,
            null,
        ) as NearJCacheConfig<*, *>

        copy5.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
        copy6.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
    }

    @Test
    fun `legacy serialized stream restores the new retry policy default`() {
        val legacy = LegacyNearJCacheConfig(
            cacheManagerFactory = SerializableCacheManagerFactory(),
            cacheName = "legacy-stream",
            frontCacheConfiguration = MutableConfiguration<String, String>(),
            isSynchronous = false,
            syncRemoteTimeout = 500L,
        )

        val restored = deserializeAsCurrent(serialize(legacy))

        restored.cacheName shouldBeEqualTo "legacy-stream"
        restored.syncRemoteRetryCount shouldBeEqualTo NearJCacheConfig.DEFAULT_SYNC_REMOTE_RETRY_COUNT
        restored.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
    }

    @Test
    fun `current serialization preserves an explicit zero retry policy`() {
        val original = NearJCacheConfig<String, String>(syncRemoteRetryCount = 0)

        val restored = deserializeAsCurrent(serialize(original))

        restored.syncRemoteRetryCount shouldBeEqualTo 0
    }

    @Test
    fun `current serialization preserves an explicit bulk policy`() {
        val original = NearJCacheConfig<String, String>(
            syncRemoteRetryCount = 0,
            bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(2),
        )

        val restored = deserializeAsCurrent(serialize(original))

        restored.syncRemoteRetryCount shouldBeEqualTo 0
        restored.bulkFrontPopulationPolicy shouldBeEqualTo original.bulkFrontPopulationPolicy
    }

    @Test
    fun `current Kotlin copy preserves a bounded policy`() {
        val original = NearJCacheConfig<String, String>(
            bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(2),
        )

        val copied = original.copy(syncRemoteRetryCount = 2)

        copied.bulkFrontPopulationPolicy shouldBeEqualTo original.bulkFrontPopulationPolicy
    }

    @Test
    fun `old five argument copy intentionally resets a bounded policy`() {
        val source = NearJCacheConfig<String, String>(
            bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(2),
        )

        val copied = source.copy(
            source.cacheManagerFactory,
            "legacy-copy",
            source.frontCacheConfiguration,
            source.isSynchronous,
            source.syncRemoteTimeout,
        )

        copied.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
    }

    @Test
    fun `explicit null policy in a serialized stream restores the safe default`() {
        val original = NearJCacheConfig<String, String>()
        NearJCacheConfig::class.java.getDeclaredField("bulkFrontPopulationPolicy").apply {
            isAccessible = true
            set(original, null)
        }

        val restored = deserializeAsCurrent(serialize(original))

        restored.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
    }

    @Test
    fun `constructor validation is restored for reflection-mutated invalid policies`() {
        listOf(0, -1).forEach { invalidCount ->
            val invalidPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(2)
            invalidPolicy.javaClass.getDeclaredField("maximumEntryCount").apply {
                isAccessible = true
                setInt(invalidPolicy, invalidCount)
            }
            val original = NearJCacheConfig<String, String>(
                cacheName = "cache-name",
                bulkFrontPopulationPolicy = invalidPolicy,
            )

            val error = assertFailsWith<InvalidObjectException> {
                deserializeAsCurrent(serialize(original))
            }
            val message = error.message.orEmpty()
            message.contains("cache-name") shouldBeEqualTo false
            message.contains("key") shouldBeEqualTo false
            message.contains("value") shouldBeEqualTo false
            message.contains("NearJCacheConfig") shouldBeEqualTo false
        }
    }

    private fun serialize(value: Any): ByteArray = ByteArrayOutputStream().use { bytes ->
        LegacyObjectOutputStream(bytes).use { output -> output.writeObject(value) }
        bytes.toByteArray()
    }

    private fun deserializeAsCurrent(bytes: ByteArray): NearJCacheConfig<String, String> =
        MappingObjectInputStream(ByteArrayInputStream(bytes)).use { input ->
            input.readObject() as NearJCacheConfig<String, String>
        }

    private class MappingObjectInputStream(input: ByteArrayInputStream) : ObjectInputStream(input) {
        override fun resolveClass(desc: ObjectStreamClass): Class<*> = super.resolveClass(desc)
    }

    private class LegacyObjectOutputStream(output: ByteArrayOutputStream) : ObjectOutputStream(output) {
        override fun writeClassDescriptor(desc: ObjectStreamClass) {
            if (desc.name != LegacyNearJCacheConfig::class.java.name) {
                super.writeClassDescriptor(desc)
                return
            }

            writeUTF(NearJCacheConfig::class.java.name)
            writeLong(1L)
            writeByte(ObjectStreamConstants.SC_SERIALIZABLE.toInt())
            val fields = desc.fields
            writeShort(fields.size)
            fields.forEach { field ->
                writeByte(field.typeCode.code)
                writeUTF(field.name)
                if (field.typeCode == 'L' || field.typeCode == '[') {
                    writeObject(field.typeString)
                }
            }
        }
    }

    private data class LegacyNearJCacheConfig(
        val cacheManagerFactory: Factory<CacheManager>,
        val cacheName: String,
        val frontCacheConfiguration: MutableConfiguration<String, String>,
        val isSynchronous: Boolean,
        val syncRemoteTimeout: Long,
    ) : Serializable {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    private class SerializableCacheManagerFactory : Factory<CacheManager>, Serializable {
        override fun create(): CacheManager = error("fixture factory must not be invoked")

        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    companion object {
        private const val FIVE_FIELD_DEFAULT_MASK = 0x1F
        private const val SIX_FIELD_DEFAULT_MASK = 0x3F
    }
}
