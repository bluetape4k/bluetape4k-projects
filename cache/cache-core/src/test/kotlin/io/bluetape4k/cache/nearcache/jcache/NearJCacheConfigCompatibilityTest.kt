package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.nearcache.jcache.management.NearJCacheManagementMXBean
import io.bluetape4k.cache.nearcache.jcache.management.NearJCacheStatisticsMXBean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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
        assertEquals(1, publicConstructors.size)
        assertEquals(
            listOf(Cache::class.java, Cache::class.java, NearJCacheConfig::class.java),
            publicConstructors.single().parameterTypes.toList(),
        )
        assertNotNull(
            NearJCacheManagementMXBean::class.java.getConstructor(NearJCache::class.java),
        )
        assertNotNull(NearJCacheStatisticsMXBean::class.java.getConstructor())
        assertEquals(
            Void.TYPE,
            NearJCacheStatisticsMXBean::class.java
                .getMethod("addRemovals", Long::class.javaPrimitiveType)
                .returnType,
        )
    }

    @Test
    fun `NearJCacheConfig data class ABI를 유지한다`() {
        val configClass = NearJCacheConfig::class.java
        val factoryType = Factory::class.java
        val configurationType = MutableConfiguration::class.java

        assertNotNull(
            configClass.getConstructor(
                factoryType,
                String::class.java,
                configurationType,
                Boolean::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
            ),
        )
        assertNotNull(
            configClass.getConstructor(
                factoryType,
                String::class.java,
                configurationType,
                Boolean::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            ),
        )

        val copy = configClass.getMethod(
            "copy",
            factoryType,
            String::class.java,
            configurationType,
            Boolean::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        )
        assertEquals(configClass, copy.returnType)
        val copyDefault = configClass.getDeclaredMethod(
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
        assertEquals(configClass, copyDefault.returnType)

        (1..6).forEach { component ->
            assertNotNull(configClass.getMethod("component$component"))
        }
        assertEquals(
            1L,
            ObjectStreamClass.lookup(configClass).serialVersionUID,
        )
    }

    @Test
    fun `prior five argument Java consumer ABI remains callable`() {
        val factory = SerializableCacheManagerFactory()
        val frontConfiguration = MutableConfiguration<String, String>()
        val configClass = NearJCacheConfig::class.java

        assertEquals(
            Int::class.javaPrimitiveType,
            configClass.getMethod("getSyncRemoteRetryCount").returnType,
        )
        assertEquals(
            Int::class.javaPrimitiveType,
            configClass.getMethod("component6").returnType,
        )

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

        assertEquals("legacy-cache", config.cacheName)
        assertEquals(NearJCacheConfig.DEFAULT_SYNC_REMOTE_RETRY_COUNT, config.syncRemoteRetryCount)
        assertEquals("legacy-copy", copy.cacheName)
        assertEquals(NearJCacheConfig.DEFAULT_SYNC_REMOTE_RETRY_COUNT, copy.syncRemoteRetryCount)
        assertEquals("kotlin-legacy-cache", kotlinConfig.cacheName)
        assertEquals("kotlin-legacy-copy", kotlinCopy.cacheName)
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

        assertEquals("legacy-stream", restored.cacheName)
        assertEquals(NearJCacheConfig.DEFAULT_SYNC_REMOTE_RETRY_COUNT, restored.syncRemoteRetryCount)
    }

    @Test
    fun `current serialization preserves an explicit zero retry policy`() {
        val original = NearJCacheConfig<String, String>(syncRemoteRetryCount = 0)

        val restored = deserializeAsCurrent(serialize(original))

        assertEquals(0, restored.syncRemoteRetryCount)
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
}
