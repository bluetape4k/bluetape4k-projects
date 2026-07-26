package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

internal class LockDocumentationTest {

    @Test
    fun `operator markers and actions stay aligned across locales`() {
        val english = readModuleFile("CoordinationLocks.md")
        val korean = readModuleFile("CoordinationLocks.ko.md")

        markerOrder(english) shouldBeEqualTo REQUIRED_OPERATOR_MARKERS
        markerOrder(korean) shouldBeEqualTo REQUIRED_OPERATOR_MARKERS

        ENGLISH_OPERATOR_FRAGMENTS.forEach { fragment ->
            english shouldContain fragment
        }
        KOREAN_OPERATOR_FRAGMENTS.forEach { fragment ->
            korean shouldContain fragment
        }

        REQUIRED_OPERATOR_MARKERS.forEach { marker ->
            val englishSection = section(english, marker)
            val koreanSection = section(korean, marker)
            OPERATOR_SIGNALS.getValue(marker).forEach { signal ->
                englishSection shouldContain signal
                koreanSection shouldContain (KOREAN_SIGNAL_EQUIVALENTS[signal] ?: signal)
            }
        }
    }

    @Test
    fun `capability migration lifecycle and non-goal contracts stay documented`() {
        val english = readModuleFile("CoordinationLocks.md")
        val korean = readModuleFile("CoordinationLocks.ko.md")

        REQUIRED_LOCK_TYPES.forEach { type ->
            english shouldContain type
            korean shouldContain type
        }
        REQUIRED_LIFECYCLE_FRAGMENTS.forEach { fragment ->
            english shouldContain fragment
            korean shouldContain fragment
        }
        REQUIRED_LEGACY_TYPES.forEach { type ->
            english shouldContain type
            korean shouldContain type
        }
        english shouldContain "not deprecated"
        korean shouldContain "deprecated가 아닙니다"
        REQUIRED_NON_GOALS.forEach { (englishFragment, koreanFragment) ->
            english shouldContain englishFragment
            korean shouldContain koreanFragment
        }
    }

    @Test
    fun `caller examples release every successful hold and reconcile ambiguous completion`() {
        listOf("CoordinationLocks.md", "CoordinationLocks.ko.md").forEach { name ->
            val content = readModuleFile(name)

            content shouldContain "is LockAcquireResult.Acquired -> releaseSuccessfully(result.handle)"
            content shouldContain "is LockAcquireResult.Reentered -> releaseSuccessfully(result.handle)"
            content shouldContain "lock.reconcileAsync(result.ownerId, result.requestId).thenCompose"
            content shouldContain "is LockReconcileResult.Owned -> lock.releaseAsync(reconciled.handle)"
            content shouldContain "is LockAcquireResult.Reentered -> suspendLock.release(result.handle)"
            content shouldContain "suspendLock.reconcile(result.ownerId, result.requestId)"
            content shouldContain "is LockReconcileResult.Owned -> suspendLock.release(reconciled.handle)"
        }
    }

    @Test
    fun `readmes and guides embed locale-specific diagrams with reader-facing alt text`() {
        val documents = mapOf(
            "README.md" to listOf(
                "infra-lettuce-diagram-03.png",
                "infra-lettuce-sequence-02.png",
            ),
            "CoordinationLocks.md" to listOf(
                "infra-lettuce-diagram-03.png",
                "infra-lettuce-sequence-02.png",
            ),
            "README.ko.md" to listOf(
                "infra-lettuce-diagram-03-ko.png",
                "infra-lettuce-sequence-02-ko.png",
            ),
            "CoordinationLocks.ko.md" to listOf(
                "infra-lettuce-diagram-03-ko.png",
                "infra-lettuce-sequence-02-ko.png",
            ),
        )

        documents.forEach { (name, expectedAssets) ->
            val content = readModuleFile(name)
            val embeds = IMAGE_PATTERN.findAll(content).map { it.groupValues[1] to it.groupValues[2] }.toList()
            expectedAssets.forEach { asset ->
                val matching = embeds.filter { (_, path) -> path.endsWith(asset) }
                matching.size shouldBeEqualTo 1
                matching.single().first.isNotBlank().shouldBeTrue()
                matching.single().first.length.shouldBeGreaterOrEqualTo(12)
            }
        }
    }

    @Test
    fun `feature tables expose all blocking and suspend Lock objects`() {
        val english = readModuleFile("README.md")
        val korean = readModuleFile("README.ko.md")

        REQUIRED_LOCK_TYPES.forEach { type ->
            english shouldContain "| `$type`"
            korean shouldContain "| `$type`"
        }
        english shouldContain "[Coordination Locks](./CoordinationLocks.md)"
        korean shouldContain "[분산 동기화 Lock](./CoordinationLocks.ko.md)"
    }

    private fun markerOrder(document: String): List<String> =
        MARKER_PATTERN.findAll(document).map { it.groupValues[1] }.toList()

    private fun section(document: String, marker: String): String {
        val start = document.indexOf("<!-- coordination-locks:$marker -->").shouldBeGreaterOrEqualTo(0)
        val next = document.indexOf("<!-- coordination-locks:", start + 1)
        return document.substring(start, if (next >= 0) next else document.length)
    }

    private fun readModuleFile(name: String): String {
        val workingDirectory = Path.of(System.getProperty("user.dir"))
        val moduleDirectory = if (workingDirectory.endsWith(Path.of("infra", "lettuce"))) {
            workingDirectory
        } else {
            workingDirectory.resolve("infra/lettuce")
        }
        return Files.readString(moduleDirectory.resolve(name))
    }

    private companion object {
        val REQUIRED_OPERATOR_MARKERS = listOf(
            "ambiguous-reconcile",
            "watchdog-leak",
            "observability",
            "alerts",
            "acl-tls",
            "namespace-migration",
            "rollout-rollback",
            "drain-cleanup",
        )

        val OPERATOR_SIGNALS = mapOf(
            "ambiguous-reconcile" to listOf("Ambiguous", "reconcile", "owner", "request"),
            "watchdog-leak" to listOf("maxLifetime", "close", "registration"),
            "observability" to listOf("LockObservationSink", "outcome", "latency"),
            "alerts" to listOf("BackendFailure", "IntegrityFailure", "OwnershipLost", "p95/p99"),
            "acl-tls" to listOf("EVALSHA", "SCRIPT LOAD", "TLS", "credential"),
            "namespace-migration" to listOf("bt4k:coord:v1", "fencing", "bounded"),
            "rollout-rollback" to listOf("Canary", "rollback", "counter", "reconcile"),
            "drain-cleanup" to listOf("drain", "bounded", "generation", "wildcard"),
        )

        val KOREAN_SIGNAL_EQUIVALENTS = mapOf(
            "bounded" to "제한",
            "Canary" to "canary",
        )

        val ENGLISH_OPERATOR_FRAGMENTS = listOf(
            "CleanupPending",
            "CapacityExceeded",
            "watchdog renewal",
            "queue saturation",
            "secret manager",
            "separate principals",
            "bounded batches",
            "preserve",
        )

        val KOREAN_OPERATOR_FRAGMENTS = listOf(
            "CleanupPending",
            "CapacityExceeded",
            "watchdog renewal",
            "queue 포화",
            "secret manager",
            "principal을 분리",
            "제한된 batch",
            "보존",
        )

        val REQUIRED_LOCK_TYPES = listOf(
            "LettuceDistributedLock",
            "LettuceSuspendDistributedLock",
            "LettuceFairLock",
            "LettuceSuspendFairLock",
            "LettuceFencedLock",
            "LettuceSuspendFencedLock",
            "LettuceReadWriteLock",
            "LettuceSuspendReadWriteLock",
            "LettuceSpinLock",
            "LettuceSuspendSpinLock",
            "LettuceMultiLock",
            "LettuceSuspendMultiLock",
        )

        val REQUIRED_LIFECYCLE_FRAGMENTS = listOf(
            "LockOwnerId",
            "LockRequestId",
            "CompletableFuture",
            "AlreadyReleased",
            "strict-greater",
            "downgrade",
            "maxAttemptsPerSecond",
            "hash tag",
            "close()",
        )

        val REQUIRED_LEGACY_TYPES = listOf(
            "LettuceLock",
            "LettuceSuspendLock",
            "LettuceFencingLease",
            "LettuceSuspendFencingLease",
            "LettuceMultiKeyLease",
            "LettuceSuspendMultiKeyLease",
        )

        val REQUIRED_NON_GOALS = listOf(
            "Java thread ownership" to "Java thread ownership",
            "indefinite wait" to "무기한 wait",
            "cross-slot best effort" to "Cross-slot best effort",
            "read-to-write upgrade" to "Read upgrade",
            "exactly-once" to "Exactly-once",
            "implicit unlock" to "implicit unlock",
        )

        val MARKER_PATTERN = Regex("<!-- coordination-locks:([a-z-]+) -->")
        val IMAGE_PATTERN = Regex("!\\[([^\\]]+)]\\(([^)]+\\.png)\\)")
    }
}
