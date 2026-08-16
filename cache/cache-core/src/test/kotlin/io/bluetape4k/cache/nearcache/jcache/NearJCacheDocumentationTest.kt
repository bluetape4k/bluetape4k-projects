package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.nearcache.jcache.management.NearJCacheConfigurationMXBean
import io.bluetape4k.cache.nearcache.jcache.management.NearJCacheTierStatisticsMXBean
import io.bluetape4k.cache.nearcache.jcache.management.registerMBeans
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import javax.cache.configuration.MutableConfiguration
import javax.management.JMX
import javax.management.MBeanServerFactory

class NearJCacheDocumentationTest {

    @Test
    fun `문서의 management 예제는 실제 Caffeine cache와 JMX proxy로 실행된다`() {
        val manager = NearJCacheConfig.CaffeineCacheManagerFactory.create()
        val suffix = UUID.randomUUID().toString()
        val configuration = MutableConfiguration<String, String>()
            .setTypes(String::class.java, String::class.java)
            .setStatisticsEnabled(true)
            .setManagementEnabled(true)
            .setStoreByValue(false)
        val front = manager.createCache("front-$suffix", configuration)
        val back = manager.createCache("back-$suffix", configuration)
        val nearCache = NearJCache(
            frontCache = front,
            backCache = back,
            config = NearJCacheConfig(frontCacheConfiguration = configuration),
        )
        val server = MBeanServerFactory.newMBeanServer()
        val registration = nearCache.registerMBeans(server, "docs-manager", "docs-cache-$suffix")

        try {
            val names = registration.activeObjectNames.associateBy { it.getKeyProperty("type") }
            val management = JMX.newMXBeanProxy(
                server,
                names.getValue("NearJCacheConfiguration"),
                NearJCacheConfigurationMXBean::class.java,
            )
            val statistics = JMX.newMXBeanProxy(
                server,
                names.getValue("NearJCacheStatistics"),
                NearJCacheTierStatisticsMXBean::class.java,
            )

            nearCache.put("42", "Ada")
            assertEquals("Ada", nearCache.get("42"))
            assertEquals("NEAR_JCACHE_WRAPPER_V1", statistics.getStatisticsScope())
            assertTrue(statistics.getSupportedOperations().contains("getAndPut"))
            assertTrue(management.isStatisticsEnabled)

            statistics.clear()
            assertEquals("Ada", nearCache.get("42"))
            nearCache.clear()
            assertEquals(null, nearCache.get("42"))
        } finally {
            nearCache.close()
            if (!back.isClosed) back.close()
            assertFalse(manager.isClosed)
        }
    }

    @Test
    fun `README와 manual locale은 같은 management API 경계를 설명한다`() {
        val documents = listOf(
            "cache/cache-core/README.md",
            "cache/cache-core/README.ko.md",
            "docs/manual/en/modules/bluetape4k-cache-core/near-cache-semantics.md",
            "docs/manual/ko/modules/bluetape4k-cache-core/near-cache-semantics.md",
        ).associateWith(::read)
        val requiredTokens = listOf(
            "setTypes",
            "setStatisticsEnabled",
            "setManagementEnabled",
            "setStoreByValue(false)",
            "registerMBeans",
            "NearJCacheConfigurationMXBean",
            "NearJCacheTierStatisticsMXBean",
            "statistics.clear()",
            "nearCache.clear()",
            "statisticsScope",
            "supportedOperations",
            "isFrontEvictionObservationSupported",
            "isBulkRemovalCountSupported",
            "isBackWriteCompletionIncluded",
            "BackCacheWriteCompletion",
            "operationId",
            "RECOVERY_REQUIRED",
            "MBeanServer",
            "NearJCacheMBeans.registerMBeans",
        )

        documents.forEach { (path, document) ->
            val section = markedSection(document)
            requiredTokens.forEach { token ->
                assertTrue(section.contains(token), "$path 문서에 $token 이(가) 없습니다")
            }
            assertFalse(STALE_CLEAR_PHRASE.containsMatchIn(section), "$path 문서에 오래된 clear 설명이 있습니다")
        }
    }

    @Test
    fun `capability matrix는 NearJCache management 지원과 unsupported 경계를 고정한다`() {
        val matrix = read("docs/cache/near-cache-capability-matrix.md")

        listOf(
            "immutable configuration snapshot",
            "logical/tier statistics",
            "explicit custom-domain JMX",
            "getAndPut",
            "getAndReplace",
            "getAndRemove",
            "loadAll",
            "invokeAll",
            "SuspendNearJCache",
            "isFrontEvictionObservationSupported=false",
            "isBulkRemovalCountSupported=false",
            "isBackWriteCompletionIncluded=false",
        ).forEach { token -> assertTrue(matrix.contains(token), "capability matrix에 $token 이(가) 없습니다") }
    }

    @Test
    fun `운영 템플릿은 lifecycle 분류와 rollout 증거 필드를 포함한다`() {
        val guide = read("docs/operations/issue-1351-nearcache-management.md")
        val template = read("docs/operations/templates/issue-1351-nearcache-management.json")

        CLASSIFICATION_STATES.forEach { state ->
            assertTrue(guide.contains(state), "운영 가이드에 $state 상태가 없습니다")
            assertTrue(template.contains("\"$state\""), "운영 템플릿에 $state 상태가 없습니다")
        }
        listOf(
            "baseSha",
            "headSha",
            "treeSha",
            "artifactIdentity",
            "configurationIdentity",
            "canaryTarget",
            "query",
            "window",
            "threshold",
            "result",
            "rollbackIdentity",
            "operationInventory",
            "cleanupEvidence",
            "owner",
            "reviewer",
        ).forEach { field -> assertTrue(template.contains("\"$field\""), "운영 템플릿에 $field 필드가 없습니다") }
        assertTrue(guide.contains("JMX 부재만으로 `DISABLED`로 분류하지 않는다"))
        assertTrue(guide.contains("RECOVERY_REQUIRED") && guide.contains("즉시 alert"))
    }

    private fun read(relativePath: String): String = Files.readString(repositoryRoot.resolve(relativePath))

    private fun markedSection(document: String): String {
        val start = document.indexOf(START_MARKER)
        val end = document.indexOf(END_MARKER)
        assertTrue(start >= 0, "$START_MARKER marker가 없습니다")
        assertTrue(end > start, "$END_MARKER marker가 없습니다")
        return document.substring(start, end + END_MARKER.length)
    }

    companion object {
        private const val START_MARKER = "<!-- issue-1351-nearcache-management:start -->"
        private const val END_MARKER = "<!-- issue-1351-nearcache-management:end -->"
        private val STALE_CLEAR_PHRASE = Regex(
            "front-only|front cache only|clear\\(\\).*front.{0,20}only|clear\\(\\).*front.{0,20}비우",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val CLASSIFICATION_STATES = listOf(
            "DISABLED",
            "NOT_REGISTERED",
            "REGISTERED",
            "RECOVERY_REQUIRED",
            "CLOSING",
            "CLOSED",
        )
        private val repositoryRoot: Path by lazy {
            generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.exists(it.resolve("settings.gradle.kts")) }
        }
    }
}
