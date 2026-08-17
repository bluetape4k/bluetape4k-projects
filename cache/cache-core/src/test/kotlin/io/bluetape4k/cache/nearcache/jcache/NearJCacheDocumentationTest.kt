package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.cache.nearcache.jcache.management.NearJCacheConfigurationMXBean
import io.bluetape4k.cache.nearcache.jcache.management.NearJCacheTierStatisticsMXBean
import io.bluetape4k.cache.nearcache.jcache.management.registerMBeans
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
            clearAuthority = NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE,
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
            nearCache.get("42") shouldBeEqualTo "Ada"
            statistics.getStatisticsScope() shouldBeEqualTo "NEAR_JCACHE_WRAPPER_V1"
            statistics.getSupportedOperations().contains("getAndPut").shouldBeTrue()
            management.isStatisticsEnabled.shouldBeTrue()
            management.getClearAuthority() shouldBeEqualTo "EXCLUSIVE_BACK_CACHE"

            statistics.clear()
            nearCache.get("42") shouldBeEqualTo "Ada"
            nearCache.clear()
            nearCache.get("42") shouldBeEqualTo null
        } finally {
            nearCache.close()
            if (!back.isClosed) back.close()
            manager.isClosed.shouldBeFalse()
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
                section.contains(token).shouldBeTrue()
            }
            STALE_CLEAR_PHRASE.containsMatchIn(section).shouldBeFalse()
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
        ).forEach { token -> matrix.contains(token).shouldBeTrue() }
    }

    @Test
    fun `운영 템플릿은 lifecycle 분류와 rollout 증거 필드를 포함한다`() {
        val guide = read("docs/operations/issue-1351-nearcache-management.md")
        val template = read("docs/operations/templates/issue-1351-nearcache-management.json")

        CLASSIFICATION_STATES.forEach { state ->
            guide.contains(state).shouldBeTrue()
            template.contains("\"$state\"").shouldBeTrue()
        }
        listOf(
            "baseSha",
            "headSha",
            "treeSha",
            "artifactIdentity",
            "configurationIdentity",
            "canaryTarget",
            "queries",
            "id",
            "query",
            "window",
            "comparator",
            "allowedDirection",
            "threshold",
            "observedResult",
            "passed",
            "result",
            "rollbackIdentity",
            "operationInventory",
            "cleanupEvidence",
            "owner",
            "reviewer",
            "bulkFrontPopulationPolicy",
            "bulkFrontPopulationMaximumEntryCount",
            "objectName",
            "externalBackReadLoad",
            "bypassReasonCounterAvailable",
            "normalMode",
            "preIssue1369ArtifactAllowed",
            "oldFrontCacheIdentity",
            "replacementFrontCacheIdentity",
            "frontCacheShared",
            "oldFrontClosed",
            "replacementFrontOpen",
            "startedAt",
            "expiresAt",
            "maximumDuration",
            "approver",
            "approvedAt",
            "frontHeapCap",
            "trafficLimit",
            "forwardFix",
            "decisionRule",
            "handoverSequence",
        ).forEach { field ->
            template.contains("\"$field\"").shouldBeTrue()
        }
        canaryQueriesAreComplete(template).shouldBeTrue()
        val lastPassed = template.lastIndexOf("\"passed\"")
        (lastPassed > 0).shouldBeTrue()
        canaryQueriesAreComplete(
            template.replaceRange(lastPassed, lastPassed + "\"passed\"".length, "\"missingPassed\""),
        ).shouldBeFalse()
        canaryQueriesAreComplete(
            template.replace("\"id\": \"external-back-read-load\"", "\"id\": \"front-hits\""),
        ).shouldBeFalse()
        FRONT_OWNERSHIP_SHAPE.containsMatchIn(template).shouldBeTrue()
        BREAK_GLASS_SHAPE.containsMatchIn(template).shouldBeTrue()
        guide.contains("JMX 부재만으로 `DISABLED`로 분류하지 않는다").shouldBeTrue()
        (guide.contains("RECOVERY_REQUIRED") && guide.contains("즉시 alert")).shouldBeTrue()
        listOf(
            "managementEnabled",
            "statisticsEnabled",
            "BYPASS_FRONT",
            "POPULATE_IF_AT_MOST",
            "FrontHits",
            "FrontMisses",
            "BackHits",
            "BackMisses",
            "AverageGetTime",
            "rollbackIdentity",
            "break-glass",
            "forward-fix",
            "판정식",
            "admission",
            "별도 front cache",
            "front cache를 공유하지",
            "모든 query마다",
            "승인자",
            "승인 시각",
            "최대 사용 시간",
        ).forEach { token -> guide.contains(token).shouldBeTrue() }
    }

    @Test
    fun `bulk getAll residency 정책은 문서와 provider DSL에서 양언어 동등하다`() {
        val documents = BULK_POLICY_DOCUMENTS.associateWith { path ->
            issue1369Section(read(path))
        }
        val requiredTokens = listOf(
            "BulkFrontPopulationPolicy.BypassFront",
            "BulkFrontPopulationPolicy.PopulateIfAtMost",
            "BYPASS_FRONT",
            "POPULATE_IF_AT_MOST",
            "bulkFrontPopulationMaximumEntryCount",
            "backValues.size",
            "legacy",
        )
        val contractTokens = listOf(
            "default-bypass",
            "bounded-all-or-nothing",
            "single-key-get-unchanged",
            "repeated-back-read",
            "legacy-safe-default",
        )

        documents.forEach { (path, section) ->
            requiredTokens.forEach { token ->
                section.contains(token).shouldBeTrue()
            }
            contractTokens.forEach { token ->
                section.contains(token).shouldBeTrue()
            }
        }

        listOf(
            "cache/cache-core/README.md" to "cache/cache-core/README.ko.md",
            "cache/cache-lettuce/README.md" to "cache/cache-lettuce/README.ko.md",
            "cache/cache-hazelcast/README.md" to "cache/cache-hazelcast/README.ko.md",
            "docs/manual/en/modules/bluetape4k-cache-core/near-cache-semantics.md" to
                    "docs/manual/ko/modules/bluetape4k-cache-core/near-cache-semantics.md",
        ).forEach { (englishPath, koreanPath) ->
            val english = documents.getValue(englishPath)
            val korean = documents.getValue(koreanPath)

            fencedCodeBlocks(english) shouldBeEqualTo fencedCodeBlocks(korean)
            numericTokens(english) shouldBeEqualTo numericTokens(korean)
            headingSignature(english) shouldBeEqualTo headingSignature(korean)
            tokenOccurrences(english) shouldBeEqualTo tokenOccurrences(korean)
        }

        documents.getValue("cache/cache-lettuce/README.md")
            .contains("LettuceCaches.nearJCache").shouldBeTrue()
        documents.getValue("cache/cache-lettuce/README.ko.md")
            .contains("LettuceCaches.nearJCache").shouldBeTrue()
        documents.getValue("cache/cache-hazelcast/README.md")
            .contains("HazelcastCaches.nearJCache").shouldBeTrue()
        documents.getValue("cache/cache-hazelcast/README.ko.md")
            .contains("HazelcastCaches.nearJCache").shouldBeTrue()

        listOf(
            "and populate the front cache with back hits",
            "back hit는 front에 populate합니다",
            "back hit를 front에 채우는",
        ).forEach { stalePhrase ->
            BULK_POLICY_DOCUMENTS.forEach { path ->
                read(path).contains(stalePhrase).shouldBeFalse()
            }
        }
    }

    @Test
    fun `clear authority marker는 EN KO 문서 쌍의 계약을 동일하게 고정한다`() {
        AUTHORITY_CONTRACT_DOCUMENTS.forEach { (englishPath, koreanPath) ->
            val english = authorityContractSection(read(englishPath))
            val korean = authorityContractSection(read(koreanPath))

            headingSignature(english) shouldBeEqualTo headingSignature(korean)
            fencedCodeBlocks(english) shouldBeEqualTo fencedCodeBlocks(korean)
            numericTokens(english) shouldBeEqualTo numericTokens(korean)
            authorityTokenOccurrences(english) shouldBeEqualTo authorityTokenOccurrences(korean)
            STALE_AUTHORITY_CLEAR_PHRASE.containsMatchIn(english).shouldBeFalse()
            STALE_AUTHORITY_CLEAR_PHRASE.containsMatchIn(korean).shouldBeFalse()
        }
    }

    private fun read(relativePath: String): String = Files.readString(repositoryRoot.resolve(relativePath))

    private fun markedSection(document: String): String {
        val start = document.indexOf(START_MARKER)
        val end = document.indexOf(END_MARKER)
        (start >= 0).shouldBeTrue()
        (end > start).shouldBeTrue()
        return document.substring(start, end + END_MARKER.length)
    }

    private fun issue1369Section(document: String): String {
        val start = document.indexOf(BULK_POLICY_START_MARKER)
        val end = document.indexOf(BULK_POLICY_END_MARKER)
        (start >= 0).shouldBeTrue()
        (end > start).shouldBeTrue()
        return document.substring(start, end + BULK_POLICY_END_MARKER.length)
    }

    private fun authorityContractSection(document: String): String {
        val start = document.indexOf(AUTHORITY_START_MARKER)
        val end = document.indexOf(AUTHORITY_END_MARKER)
        (start >= 0).shouldBeTrue()
        (end > start).shouldBeTrue()
        return document.substring(start, end + AUTHORITY_END_MARKER.length)
    }

    private fun fencedCodeBlocks(document: String): List<String> = FENCED_CODE.findAll(document)
        .map { it.groupValues[1].trim() }
        .toList()

    private fun numericTokens(document: String): List<String> = NUMBER.findAll(document)
        .map { it.value }
        .toList()

    private fun headingSignature(document: String): List<Int> = HEADING.findAll(document)
        .map { it.value.indexOf(' ') }
        .toList()

    private fun tokenOccurrences(document: String): Map<String, Int> = PARITY_TOKENS.associateWith { token ->
        document.windowed(token.length).count(token::equals)
    }

    private fun authorityTokenOccurrences(document: String): Map<String, Int> =
        AUTHORITY_PARITY_TOKENS.associateWith { token ->
            document.windowed(token.length).count(token::equals)
        }

    private fun canaryQueriesAreComplete(template: String): Boolean {
        val queries = flatJsonObjectsInArray(template, "queries")
        if (queries.size != EXPECTED_CANARY_QUERY_IDS.size) return false

        val ids = queries.mapNotNull { jsonStringValue(it, "id") }
        if (ids.size != ids.toSet().size || ids.toSet() != EXPECTED_CANARY_QUERY_IDS) return false
        if (queries.any { query -> REQUIRED_CANARY_QUERY_FIELDS.any { "\"$it\"" !in query } }) return false

        val windows = queries.mapNotNull { jsonStringValue(it, "window") }
        return windows.size == queries.size && windows.distinct().size == 1
    }

    private fun flatJsonObjectsInArray(document: String, field: String): List<String> {
        val fieldStart = document.indexOf("\"$field\"")
        if (fieldStart < 0) return emptyList()
        val arrayStart = document.indexOf('[', fieldStart)
        if (arrayStart < 0) return emptyList()
        val arrayEnd = document.indexOf(']', arrayStart)
        if (arrayEnd < 0) return emptyList()
        return FLAT_JSON_OBJECT.findAll(document.substring(arrayStart + 1, arrayEnd))
            .map { it.value }
            .toList()
    }

    private fun jsonStringValue(document: String, field: String): String? = Regex(
        """\"${Regex.escape(field)}\"\s*:\s*\"([^\"]+)\"""",
    ).find(document)?.groupValues?.get(1)

    companion object {
        private const val START_MARKER = "<!-- issue-1351-nearcache-management:start -->"
        private const val END_MARKER = "<!-- issue-1351-nearcache-management:end -->"
        private const val BULK_POLICY_START_MARKER = "<!-- issue-1369-bulk-policy:start -->"
        private const val BULK_POLICY_END_MARKER = "<!-- issue-1369-bulk-policy:end -->"
        private const val AUTHORITY_START_MARKER = "<!-- nearjcache-clear-authority-contract -->"
        private const val AUTHORITY_END_MARKER = "<!-- /nearjcache-clear-authority-contract -->"
        private val FENCED_CODE = Regex("```(?:kotlin)?\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
        private val NUMBER = Regex("\\b\\d[\\d_]*\\b")
        private val HEADING = Regex("^#{1,6} ", RegexOption.MULTILINE)
        private val PARITY_TOKENS = listOf(
            "BulkFrontPopulationPolicy.BypassFront",
            "BulkFrontPopulationPolicy.PopulateIfAtMost",
            "BYPASS_FRONT",
            "POPULATE_IF_AT_MOST",
            "bulkFrontPopulationMaximumEntryCount",
            "backValues.size",
            "legacy",
            "getAll",
            "get()",
            "default-bypass",
            "bounded-all-or-nothing",
            "single-key-get-unchanged",
            "repeated-back-read",
            "legacy-safe-default",
            "LettuceCaches.nearJCache",
            "HazelcastCaches.nearJCache",
        )
        private val STALE_CLEAR_PHRASE = Regex(
            "front-only|front cache only|clear\\(\\).*front.{0,20}only|clear\\(\\).*front.{0,20}비우",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val STALE_AUTHORITY_CLEAR_PHRASE = Regex(
            "front[ -]only|프론트만|front-only",
            RegexOption.IGNORE_CASE,
        )
        private val AUTHORITY_PARITY_TOKENS = listOf(
            "NearJCacheClearAuthority",
            "DENY",
            "EXCLUSIVE_BACK_CACHE",
            "clearAllCache",
            "removeAll",
            "SecurityException",
            "NearJCache",
            "LettuceCaches.nearJCache",
            "HazelcastCaches.nearJCache",
            "RedissonCaches.nearJCache",
            "128",
            "1368",
            "1369",
        )
        private val FLAT_JSON_OBJECT = Regex("""\{[^{}]*}""", RegexOption.DOT_MATCHES_ALL)
        private val EXPECTED_CANARY_QUERY_IDS = setOf(
            "front-hits",
            "front-misses",
            "back-hits",
            "back-misses",
            "average-get-time",
            "external-back-read-load",
        )
        private val REQUIRED_CANARY_QUERY_FIELDS = setOf(
            "id",
            "query",
            "window",
            "comparator",
            "allowedDirection",
            "threshold",
            "observedResult",
            "passed",
        )
        private val FRONT_OWNERSHIP_SHAPE = Regex(
            """\"frontOwnership\"\s*:\s*\{[^}]*\"oldFrontCacheIdentity\"[^}]*""" +
                    """\"replacementFrontCacheIdentity\"[^}]*\"frontCacheShared\"\s*:\s*false[^}]*""" +
                    """\"oldFrontClosed\"[^}]*\"replacementFrontOpen\"""",
            RegexOption.DOT_MATCHES_ALL,
        )
        private val BREAK_GLASS_SHAPE = Regex(
            """\"breakGlass\"\s*:\s*\{[^}]*\"enabled\"[^}]*\"startedAt\"[^}]*\"expiresAt\"[^}]*""" +
                    """\"maximumDuration\"[^}]*\"approver\"[^}]*\"approvedAt\"""",
            RegexOption.DOT_MATCHES_ALL,
        )
        private val CLASSIFICATION_STATES = listOf(
            "DISABLED",
            "NOT_REGISTERED",
            "REGISTERED",
            "RECOVERY_REQUIRED",
            "CLOSING",
            "CLOSED",
        )
        private val BULK_POLICY_DOCUMENTS = listOf(
            "cache/cache-core/README.md",
            "cache/cache-core/README.ko.md",
            "cache/cache-lettuce/README.md",
            "cache/cache-lettuce/README.ko.md",
            "cache/cache-hazelcast/README.md",
            "cache/cache-hazelcast/README.ko.md",
            "docs/manual/en/modules/bluetape4k-cache-core/near-cache-semantics.md",
            "docs/manual/ko/modules/bluetape4k-cache-core/near-cache-semantics.md",
            "docs/cache/near-cache-capability-matrix.md",
        )
        private val AUTHORITY_CONTRACT_DOCUMENTS = listOf(
            "cache/cache-core/README.md" to "cache/cache-core/README.ko.md",
            "cache/cache-lettuce/README.md" to "cache/cache-lettuce/README.ko.md",
            "cache/cache-hazelcast/README.md" to "cache/cache-hazelcast/README.ko.md",
            "cache/cache-redisson/README.md" to "cache/cache-redisson/README.ko.md",
            "docs/manual/en/modules/bluetape4k-cache-core/near-cache-semantics.md" to
                    "docs/manual/ko/modules/bluetape4k-cache-core/near-cache-semantics.md",
            "docs/manual/en/modules/bluetape4k-cache-hazelcast/jcache-near-cache-serialization.md" to
                    "docs/manual/ko/modules/bluetape4k-cache-hazelcast/jcache-near-cache-serialization.md",
        )
        private val repositoryRoot: Path by lazy {
            generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.exists(it.resolve("settings.gradle.kts")) }
        }
    }
}
