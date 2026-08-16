package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeBlank
import org.junit.jupiter.api.Test
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import javax.cache.configuration.MutableConfiguration

class NearJCacheConfigBinaryCompatibilityTest {

    @Test
    fun `pinned precompiled consumers preserve legacy and current linkage`() {
        val legacyJar = resource("nearjcache-config-1.12.1-consumers.jar")
        val pre1369Jar = resource("nearjcache-config-pre-1369-consumers.jar")
        verifyManifest(legacyJar, pre1369Jar)

        URLClassLoader(
            arrayOf(legacyJar.toURI().toURL(), pre1369Jar.toURI().toURL()),
            javaClass.classLoader,
        ).use { loader ->
            verifyKotlinConsumers(loader)
            verifyJavaConsumers(loader)
        }
    }

    private fun verifyKotlinConsumers(loader: ClassLoader) {
        val legacy = load(loader, "$FIXTURE_PACKAGE.NearJCacheConfigConsumer1121")
        val pre1369 = load(loader, "$FIXTURE_PACKAGE.NearJCacheConfigConsumerPre1369")
        val legacyConfig = invoke(legacy, "constructWithDefaults") as NearJCacheConfig<*, *>
        val legacyCopy = invoke(legacy, "copyWithDefaults", legacyConfig) as NearJCacheConfig<*, *>
        val pre1369Config = invoke(pre1369, "constructWithDefaults") as NearJCacheConfig<*, *>
        val pre1369Copy = invoke(pre1369, "copyWithDefaults", pre1369Config) as NearJCacheConfig<*, *>

        legacyConfig.cacheName shouldBeEqualTo "legacy-kotlin"
        legacyCopy.cacheName shouldBeEqualTo "legacy-kotlin-copy"
        pre1369Config.cacheName shouldBeEqualTo "pre-1369"
        pre1369Config.syncRemoteRetryCount shouldBeEqualTo 2
        pre1369Copy.syncRemoteRetryCount shouldBeEqualTo 2
        listOf(legacyConfig, legacyCopy, pre1369Config, pre1369Copy).forEach { config ->
            config.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
        }
    }

    private fun verifyJavaConsumers(loader: ClassLoader) {
        val legacy = load(loader, "$FIXTURE_PACKAGE.NearJCacheConfigConsumer1121Java")
        val pre1369 = load(loader, "$FIXTURE_PACKAGE.NearJCacheConfigConsumerPre1369Java")
        val legacyDefault = invoke(legacy, "constructWithNoArguments") as NearJCacheConfig<*, *>
        val pre1369Default = invoke(pre1369, "constructWithNoArguments") as NearJCacheConfig<*, *>

        legacyDefault.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
        pre1369Default.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
        verifyLegacyJavaDirectCalls(legacy)
        verifyPre1369JavaDirectCalls(pre1369)
    }

    private fun verifyLegacyJavaDirectCalls(legacy: Class<*>) {
        val frontConfiguration = MutableConfiguration<String, String>()
        val factory = NearJCacheConfig.CaffeineCacheManagerFactory
        val config = invoke(
            legacy,
            "constructWithFiveArguments",
            factory,
            "legacy-java",
            frontConfiguration,
            true,
            250L,
        ) as NearJCacheConfig<*, *>
        val copy = invoke(
            legacy,
            "copyWithFiveArguments",
            config,
            factory,
            "legacy-java-copy",
            frontConfiguration,
            false,
            500L,
        ) as NearJCacheConfig<*, *>

        config.cacheName shouldBeEqualTo "legacy-java"
        copy.cacheName shouldBeEqualTo "legacy-java-copy"
        config.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
        copy.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
    }

    private fun verifyPre1369JavaDirectCalls(pre1369: Class<*>) {
        val frontConfiguration = MutableConfiguration<String, String>()
        val factory = NearJCacheConfig.CaffeineCacheManagerFactory
        val config = invoke(
            pre1369,
            "constructWithSixArguments",
            factory,
            "pre-java",
            frontConfiguration,
            true,
            250L,
            2,
        ) as NearJCacheConfig<*, *>
        val copy = invoke(
            pre1369,
            "copyWithSixArguments",
            config,
            factory,
            "pre-java-copy",
            frontConfiguration,
            false,
            500L,
            3,
        ) as NearJCacheConfig<*, *>

        config.syncRemoteRetryCount shouldBeEqualTo 2
        copy.syncRemoteRetryCount shouldBeEqualTo 3
        config.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
        copy.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
    }

    private fun verifyManifest(legacyJar: File, pre1369Jar: File) {
        val manifest = resource("manifest.json").readText()
        manifest.shouldNotBeBlank()
        sha256(legacyJar) shouldBeEqualTo extractHash(manifest, "nearjcache-config-1.12.1-consumers.jar")
        sha256(pre1369Jar) shouldBeEqualTo extractHash(manifest, "nearjcache-config-pre-1369-consumers.jar")
        listOf(
            "NearJCacheConfigConsumer1121.kt",
            "NearJCacheConfigConsumer1121.java",
            "NearJCacheConfigConsumerPre1369.kt",
            "NearJCacheConfigConsumerPre1369.java",
        ).forEach { sourceName ->
            val source = resource("src/$sourceName")
            sha256(source) shouldBeEqualTo extractHash(manifest, sourceName)
        }
        val currentSource = File(CURRENT_SOURCE_PATH)
        sha256(currentSource) shouldBeEqualTo extractHash(manifest, currentSource.name)
        extractObjectField(manifest, "pre-1369", "commit") shouldBeEqualTo PRE_1369_COMMIT
        extractObjectField(manifest, "pre-1369", "artifactSha256") shouldBeEqualTo PRE_1369_ARTIFACT_SHA
        extractObjectField(manifest, "1.12.1", "artifactSha256") shouldBeEqualTo RELEASE_1121_ARTIFACT_SHA
        extractObjectField(manifest, "cache-api", "artifactSha256") shouldBeEqualTo CACHE_API_ARTIFACT_SHA
        extractObjectField(manifest, "compilers", "java") shouldBeEqualTo JAVA_VERSION
        manifest.contains("copy\$default") shouldBeEqualTo true
        Path.of("..", "..", ".github", "pull_request_template.md").normalize().toFile().readText()
            .contains("prior-release ABI(Java/Kotlin) fixture") shouldBeEqualTo true
        verifyJarEntry(legacyJar, "$FIXTURE_PATH/NearJCacheConfigConsumer1121.class")
        verifyJarEntry(pre1369Jar, "$FIXTURE_PATH/NearJCacheConfigConsumerPre1369.class")
    }

    private fun extractHash(manifest: String, name: String): String {
        val line = manifest.lineSequence().first { it.contains("\"$name\"") }
        return line.substringAfter("\"sha256\": \"").substringBefore('"')
    }

    private fun extractObjectField(manifest: String, objectName: String, fieldName: String): String {
        val objectBody = Regex("\\\"${Regex.escape(objectName)}\\\"\\s*:\\s*\\{([^}]*)}")
            .find(manifest)
            ?.groupValues
            ?.get(1)
            ?: error("Missing manifest object: $objectName")
        val encodedValue = Regex(
            "\\\"${Regex.escape(fieldName)}\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"",
        )
            .find(objectBody)
            ?.groupValues
            ?.get(1)
            ?: error("Missing manifest field: $objectName.$fieldName")
        return encodedValue.replace("\\\"", "\"").replace("\\\\", "\\")
    }

    private fun verifyJarEntry(jar: File, expectedEntry: String) {
        java.util.jar.JarFile(jar).use { file ->
            (file.getJarEntry(expectedEntry) != null) shouldBeEqualTo true
        }
    }

    private fun resource(name: String): File =
        javaClass.getResource("/compat/issue-1369/$name")?.toURI()?.let(::File)
            ?: error("Missing compatibility fixture resource: $name")

    private fun load(loader: ClassLoader, name: String): Class<*> = Class.forName(name, true, loader)

    private fun invoke(type: Class<*>, name: String, vararg args: Any?): Any? = try {
        val method = type.declaredMethods.first { it.name == name && it.parameterCount == args.size }
            .apply { isAccessible = true }
        method.invoke(null, *args)
    } catch (error: InvocationTargetException) {
        throw (error.cause ?: error)
    }

    private fun sha256(file: File): String = sha256(Files.readAllBytes(file.toPath()))

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    companion object {
        private const val FIXTURE_PACKAGE = "io.bluetape4k.cache.nearcache.jcache.compat.issue1369"
        private const val FIXTURE_PATH = "io/bluetape4k/cache/nearcache/jcache/compat/issue1369"
        private const val CURRENT_SOURCE_PATH =
            "src/test/java/io/bluetape4k/cache/nearcache/jcache/CurrentNearJCacheConfigConsumer.java"
        private const val PRE_1369_COMMIT = "05e3174ac11fc488a8c1ebc6027df3759271aa55"
        private const val PRE_1369_ARTIFACT_SHA = "bea720dc5cc3927092d104174101013048195915736279be43e696495d10d9cd"
        private const val RELEASE_1121_ARTIFACT_SHA =
            "0943cea523c581b82ecdbef700a9e8629e669ceca60cfd1d6ba02986d39d59e6"
        private const val CACHE_API_ARTIFACT_SHA =
            "9f34e007edfa82a7b2a2e1b969477dcf5099ce7f4f926fb54ce7e27c4a0cd54b"
        private const val JAVA_VERSION = "java version \"25.0.4\" 2026-07-21 LTS"
    }
}
