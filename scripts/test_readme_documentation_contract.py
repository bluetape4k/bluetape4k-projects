from __future__ import annotations

import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(relative_path: str) -> str:
    return (ROOT / relative_path).read_text(encoding="utf-8")


def section(document: str, marker: str, prefix: str) -> str:
    start = document.index(f"<!-- {prefix}:{marker} -->")
    next_marker = document.find(f"<!-- {prefix}:", start + 1)
    return document[start : next_marker if next_marker >= 0 else len(document)]


def assert_ordered_terms(test: unittest.TestCase, document: str, terms: tuple[str, ...]) -> None:
    cursor = 0
    for term in terms:
        next_index = document.lower().find(term.lower(), cursor)
        test.assertGreaterEqual(next_index, cursor, f"문서에서 순서가 어긋난 항목: {term}")
        cursor = next_index + len(term)


class ReadmeDocumentationContractTest(unittest.TestCase):
    def test_hibernate_lettuce_readmes_use_consumer_coordinates(self) -> None:
        cases = (
            (
                "spring-boot/hibernate-lettuce",
                ("Libs.", "libs.", "springBootStarter", 'springBoot("hibernate")', "micrometer_core"),
                (
                    "org.springframework.boot:spring-boot-dependencies",
                    "io.github.bluetape4k:bluetape4k-spring-boot-hibernate-lettuce",
                    "org.springframework.boot:spring-boot-starter-data-jpa",
                    "org.springframework.boot:spring-boot-starter-actuator",
                    "io.micrometer:micrometer-core",
                    "org.springframework.boot:spring-boot-hibernate",
                ),
            ),
            (
                "examples/hibernate-lettuce-demo",
                ("Libs.", "libs.", "springBootStarter", 'springBoot("hibernate")', "h2_database", "testcontainers_junit5"),
                (
                    "org.springframework.boot:spring-boot-dependencies",
                    "io.github.bluetape4k:bluetape4k-spring-boot-hibernate-lettuce",
                    "org.springframework.boot:spring-boot-starter-web",
                    "org.springframework.boot:spring-boot-starter-data-jpa",
                    "org.springframework.boot:spring-boot-starter-actuator",
                    "com.h2database:h2",
                    "org.springframework.boot:spring-boot-starter-test",
                ),
            ),
        )
        for relative_directory, forbidden, required in cases:
            for locale in ("README.md", "README.ko.md"):
                text = read(f"{relative_directory}/{locale}")
                for fragment in forbidden:
                    self.assertNotIn(fragment, text, f"{relative_directory}/{locale}: {fragment}")
                for fragment in required:
                    self.assertIn(fragment, text, f"{relative_directory}/{locale}: {fragment}")

    def test_cassandra_readmes_use_current_coroutine_repository_signatures(self) -> None:
        forbidden = (
            "CoroutineCrudRepository<Person, UUID>",
            "suspend fun findByLastName(lastName: String): Flow<Person>",
            "findByLastName",
        )
        required = (
            "CoroutineCrudRepository<Person, String>",
            "fun findByLastname(lastname: String): Flow<Person>",
            "suspend fun findByFirstnameAndLastname(firstname: String, lastname: String): Person?",
        )
        for locale in ("README.md", "README.ko.md"):
            text = read(f"examples/cassandra-demo/{locale}")
            for fragment in forbidden:
                self.assertNotIn(fragment, text, f"{locale}: {fragment}")
            for fragment in required:
                self.assertIn(fragment, text, f"{locale}: {fragment}")

    def test_geo_readmes_do_not_expose_stale_or_internal_references(self) -> None:
        forbidden = (
            "io.bluetape4k.geo.geohash",
            "io.bluetape4k.geo.geocode",
            "io.bluetape4k.geo.geoip2",
            "GoogleGeocoder",
            "GeoIp2Support",
            "Libs.",
        )
        for locale in ("README.md", "README.ko.md"):
            text = read(f"utils/geo/{locale}")
            for fragment in forbidden:
                self.assertNotIn(fragment, text, f"{locale}: {fragment}")

    def test_mock_webflux_readmes_do_not_list_unimplemented_routes(self) -> None:
        stale_routes = (
            "/admin/info", "/httpbin/stream-bytes/{n}", "/httpbin/drip", "/httpbin/sse",
            "/httpbin/brotli", "/httpbin/html", "/httpbin/xml", "/httpbin/json",
            "/httpbin/robots.txt", "/httpbin/deny",
        )
        for locale in ("README.md", "README.ko.md"):
            text = read(f"testing/mock-webflux-server/{locale}")
            for route in stale_routes:
                self.assertNotIn(route, text, f"{locale}: {route}")

    def test_webclient_readmes_do_not_call_retrieve_twice(self) -> None:
        pattern = re.compile(r"http(?:Get|Post)\([^)]*\)\s*\.\s*retrieve\(\)")
        for locale in ("README.md", "README.ko.md"):
            text = read(f"spring-boot/core/{locale}")
            matches = [re.sub(r"\s+", " ", match.group(0)) for match in pattern.finditer(text)]
            self.assertEqual([], matches, f"{locale}에서 WebClient retrieve 중복 호출: {matches}")


class LockDocumentationContractTest(unittest.TestCase):
    REQUIRED_OPERATOR_MARKERS = (
        "ambiguous-reconcile", "watchdog-leak", "observability", "alerts", "acl-tls", "namespace-migration",
        "rollout-rollback", "drain-cleanup",
    )
    OPERATOR_SIGNALS = {
        "ambiguous-reconcile": ("Ambiguous", "reconcile", "owner", "request"),
        "watchdog-leak": ("maxLifetime", "close", "registration"),
        "observability": ("LockObservationSink", "outcome", "latency"),
        "alerts": ("BackendFailure", "IntegrityFailure", "OwnershipLost", "p95/p99"),
        "acl-tls": ("EVALSHA", "SCRIPT LOAD", "TLS", "credential"),
        "namespace-migration": ("bt4k:coord:v1", "fencing", "bounded"),
        "rollout-rollback": ("Canary", "rollback", "counter", "reconcile"),
        "drain-cleanup": ("drain", "bounded", "generation", "wildcard"),
    }
    KOREAN_SIGNAL_EQUIVALENTS = {"bounded": "제한", "Canary": "canary"}
    REQUIRED_LOCK_TYPES = (
        "LettuceDistributedLock", "LettuceSuspendDistributedLock", "LettuceFairLock", "LettuceSuspendFairLock",
        "LettuceFencedLock", "LettuceSuspendFencedLock", "LettuceReadWriteLock", "LettuceSuspendReadWriteLock",
        "LettuceSpinLock", "LettuceSuspendSpinLock", "LettuceMultiLock", "LettuceSuspendMultiLock",
    )
    REQUIRED_LIFECYCLE_FRAGMENTS = (
        "LockOwnerId", "LockRequestId", "CompletableFuture", "AlreadyReleased", "strict-greater", "downgrade",
        "maxAttemptsPerSecond", "hash tag", "close()",
    )
    REQUIRED_LEGACY_TYPES = (
        "LettuceLock", "LettuceSuspendLock", "LettuceFencingLease", "LettuceSuspendFencingLease",
        "LettuceMultiKeyLease", "LettuceSuspendMultiKeyLease",
    )
    REQUIRED_NON_GOALS = (
        ("Java thread ownership", "Java thread ownership"), ("indefinite wait", "무기한 wait"),
        ("cross-slot best effort", "Cross-slot best effort"), ("read-to-write upgrade", "Read upgrade"),
        ("exactly-once", "Exactly-once"), ("implicit unlock", "implicit unlock"),
    )

    def documents(self) -> dict[str, str]:
        return {locale: read(f"infra/lettuce/{locale}") for locale in (
            "README.md", "README.ko.md", "CoordinationLocks.md", "CoordinationLocks.ko.md",
        )}

    def test_operator_markers_and_actions_stay_aligned(self) -> None:
        documents = self.documents()
        marker_pattern = re.compile(r"<!-- coordination-locks:([a-z-]+) -->")
        english = documents["CoordinationLocks.md"]
        korean = documents["CoordinationLocks.ko.md"]
        self.assertEqual(self.REQUIRED_OPERATOR_MARKERS, tuple(marker_pattern.findall(english)))
        self.assertEqual(self.REQUIRED_OPERATOR_MARKERS, tuple(marker_pattern.findall(korean)))
        for fragment in ("CleanupPending", "CapacityExceeded", "watchdog renewal", "queue saturation", "secret manager", "separate principals", "bounded batches", "preserve"):
            self.assertIn(fragment, english)
        for fragment in ("CleanupPending", "CapacityExceeded", "watchdog renewal", "queue 포화", "secret manager", "principal을 분리", "제한된 batch", "보존"):
            self.assertIn(fragment, korean)
        for marker, signals in self.OPERATOR_SIGNALS.items():
            english_section = section(english, marker, "coordination-locks")
            korean_section = section(korean, marker, "coordination-locks")
            for signal in signals:
                self.assertIn(signal, english_section)
                self.assertIn(self.KOREAN_SIGNAL_EQUIVALENTS.get(signal, signal), korean_section)

    def test_lock_lifecycle_and_non_goal_contracts_stay_documented(self) -> None:
        documents = self.documents()
        english = documents["CoordinationLocks.md"]
        korean = documents["CoordinationLocks.ko.md"]
        for fragment in self.REQUIRED_LOCK_TYPES + self.REQUIRED_LIFECYCLE_FRAGMENTS + self.REQUIRED_LEGACY_TYPES:
            self.assertIn(fragment, english)
            self.assertIn(fragment, korean)
        self.assertIn("not deprecated", english)
        self.assertIn("deprecated가 아닙니다", korean)
        for english_fragment, korean_fragment in self.REQUIRED_NON_GOALS:
            self.assertIn(english_fragment, english)
            self.assertIn(korean_fragment, korean)

    def test_lock_examples_completion_holds_and_reconcile_ambiguous_completion(self) -> None:
        for locale in ("CoordinationLocks.md", "CoordinationLocks.ko.md"):
            content = read(f"infra/lettuce/{locale}")
            for fragment in (
                "is LockAcquireResult.Acquired -> releaseSuccessfully(result.handle)",
                "is LockAcquireResult.Reentered -> releaseSuccessfully(result.handle)",
                "lock.reconcileAsync(result.ownerId, result.requestId).thenCompose",
                "is LockReconcileResult.Owned -> lock.releaseAsync(reconciled.handle)",
                "is LockAcquireResult.Reentered -> suspendLock.release(result.handle)",
                "suspendLock.reconcile(result.ownerId, result.requestId)",
                "is LockReconcileResult.Owned -> suspendLock.release(reconciled.handle)",
            ):
                self.assertIn(fragment, content)

    def test_lock_documents_embed_locale_specific_diagrams_with_alt_text(self) -> None:
        expected_assets = {
            "README.md": ("infra-lettuce-diagram-03.png", "infra-lettuce-sequence-02.png"),
            "CoordinationLocks.md": ("infra-lettuce-diagram-03.png", "infra-lettuce-sequence-02.png"),
            "README.ko.md": ("infra-lettuce-diagram-03-ko.png", "infra-lettuce-sequence-02-ko.png"),
            "CoordinationLocks.ko.md": ("infra-lettuce-diagram-03-ko.png", "infra-lettuce-sequence-02-ko.png"),
        }
        pattern = re.compile(r"!\[([^\]]+)]\(([^)]+\.png)\)")
        for locale, assets in expected_assets.items():
            embeds = [(match.group(1), match.group(2)) for match in pattern.finditer(read(f"infra/lettuce/{locale}"))]
            for asset in assets:
                matching = [(alt, path) for alt, path in embeds if path.endswith(asset)]
                self.assertEqual(1, len(matching), f"{locale}: {asset}")
                self.assertGreaterEqual(len(matching[0][0]), 12)

    def test_lock_feature_tables_expose_all_lock_objects(self) -> None:
        english = read("infra/lettuce/README.md")
        korean = read("infra/lettuce/README.ko.md")
        for lock_type in self.REQUIRED_LOCK_TYPES:
            self.assertIn(f"| `{lock_type}`", english)
            self.assertIn(f"| `{lock_type}`", korean)
        self.assertIn("[Coordination Locks](./CoordinationLocks.md)", english)
        self.assertIn("[분산 동기화 Lock](./CoordinationLocks.ko.md)", korean)


class LeaseDocumentationContractTest(unittest.TestCase):
    def test_multi_key_lease_readmes_preserve_policy_structure(self) -> None:
        markers = ("basic", "resilience", "recovery", "security-telemetry", "migration", "lost-token")
        policy = (
            ".withRetry(retry)", ".withCircuitBreaker(circuitBreaker)", ".withBulkhead(bulkhead)", "Duration.ofMillis(50)",
            ".slidingWindowSize(20)", ".minimumNumberOfCalls(10)", ".maxConcurrentCalls(32)", "Duration.ofMillis(100)",
            "operation", "result", "exception", "key/token", "JWT", "PII", "ACL", "TLS", "dual-write",
            "CompletableFuture", "caller wait", "Redis server execution",
        )
        contracts = {
            "recovery": ("acquire", "Acquired", "AlreadyOwned", "PartialOwnership", "Conflicted", "inspect", "Owned", "Lost", "PartialOwnership", "Conflicted", "renew", "Renewed", "PartialLoss", "Lost", "OwnershipMismatch", "release", "Released", "PartialRelease", "Lost", "OwnershipMismatch"),
            "migration": ("1.", "shared slot", "durable", "2.", "writer", "3.", "TTL", "token", "4.", "namespace", "hash-tag", "token", "5.", "dual-write", "6.", "rollback"),
            "lost-token": ("persistent key", "MultiKeyLeaseIntegrityException", "namespace/key", "namespace", "durable authority"),
        }
        marker_pattern = re.compile(r"<!-- multi-key-lease:([a-z-]+) -->")
        heading_pattern = re.compile(r"<!-- multi-key-lease:([a-z-]+) -->\s*\n(#{3,4}) [^\n]+")
        for locale in ("README.md", "README.ko.md"):
            document = read(f"infra/lettuce/{locale}")
            self.assertEqual(markers, tuple(marker_pattern.findall(document)))
            self.assertEqual((3, 4, 4, 4, 4, 4), tuple(len(match.group(2)) for match in heading_pattern.finditer(document)))
            for fragment in policy:
                self.assertIn(fragment, document)
            for marker, terms in contracts.items():
                assert_ordered_terms(self, section(document, marker, "multi-key-lease"), terms)


class FencingLeaseDocumentationContractTest(unittest.TestCase):
    REQUIRED_MARKERS = ("basic", "downstream-guard", "resilience", "recovery", "diagnostics", "caller-actions", "security-telemetry", "limitations")
    REQUIRED_POLICY_FRAGMENTS = (
        "LettuceFencingLeaseConfig", "namespace", "resourceName", "epoch", "bootstrap", "CounterUnavailable", "(epoch, sequence)", "affectedRows == 1", ".withRetry(retry)", ".withCircuitBreaker(circuitBreaker)", ".withBulkhead(bulkhead)", "BackendFailure", "pause", "drain", "CAS", "readiness", "rollout", "resume", "EVAL_RO", "LEASE_NO_TTL", "operation", "result", "kind", "namespace/resource/owner/token/fingerprint", "exactly-once", "business idempotency", "durable correctness",
    )
    REQUIRED_SECTION_CONTRACTS = {
        "resilience": (".maxAttempts(", ".waitDuration(", ".retryOnResult", ".retryOnException", ".slidingWindowSize(", ".minimumNumberOfCalls(", ".failureRateThreshold(", ".recordResult", ".ignoreException", ".maxConcurrentCalls(", ".maxWaitDuration(", ".withRetry(retry)", ".withCircuitBreaker(circuitBreaker)", ".withBulkhead(bulkhead)"),
        "caller-actions": ("Initialized", "AlreadyInitialized", "Acquired", "AlreadyOwned", "Owned", "Renewed", "Released", "acquire", "Contended", "inspect", "Contended", "Lost", "OwnershipMismatch", "CounterUnavailable", "SequenceExhausted", "IntegrityFailure", "BackendFailure"),
        "recovery": ("pause", "block old acquire", "drain", "CAS", "bootstrap", "readiness", "rollout", "confirm old absence", "resume"),
    }
    REQUIRED_KDOC_FRAGMENTS = ("durable external authority", "ambiguous", "same owner ID", "stable resource identity", "not strictly greater", "Redis Cluster", "ordering, not durable business correctness", "must not be lowered")

    def test_fencing_readmes_preserve_static_safety_contract(self) -> None:
        marker_pattern = re.compile(r"<!-- fencing-lease:([a-z-]+) -->")
        heading_pattern = re.compile(r"<!-- fencing-lease:([a-z-]+) -->\s*\n(#{3,4}) [^\n]+")
        for locale in ("README.md", "README.ko.md"):
            document = read(f"infra/lettuce/{locale}")
            self.assertEqual(self.REQUIRED_MARKERS, tuple(marker_pattern.findall(document)))
            self.assertEqual((3, 4, 4, 4, 4, 4, 4, 4), tuple(len(match.group(2)) for match in heading_pattern.finditer(document)))
            for fragment in self.REQUIRED_POLICY_FRAGMENTS:
                self.assertIn(fragment, document)
            for marker, terms in self.REQUIRED_SECTION_CONTRACTS.items():
                assert_ordered_terms(self, section(document, marker, "fencing-lease"), terms)
        english = read("infra/lettuce/README.md")
        korean = read("infra/lettuce/README.ko.md")
        for marker, language in (("resilience", "kotlin"), ("downstream-guard", "sql"), ("diagnostics", "lua")):
            self.assertEqual(self.code_block(section(english, marker, "fencing-lease"), language), self.code_block(section(korean, marker, "fencing-lease"), language))

    def test_fencing_public_api_has_kdoc_for_caller_owned_boundaries(self) -> None:
        source_paths = (
            "src/main/kotlin/io/bluetape4k/redis/lettuce/lease/FencingLeaseValue.kt",
            "src/main/kotlin/io/bluetape4k/redis/lettuce/lease/FencingLeaseResult.kt",
            "src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceFencingLease.kt",
            "src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceSuspendFencingLease.kt",
        )
        declaration_pattern = re.compile(r"(?m)^[ \t]*(?!(?:private|internal|protected)\b)(?:public\s+)?(?:(?:data\s+)?(?:class|object)\s+\w+|enum\s+class\s+\w+|sealed\s+interface\s+\w+|companion\s+object|constructor\s*\(|(?:override\s+)?(?:suspend\s+)?fun\s+\w+\s*\()")
        property_pattern = re.compile(r"\bval\s+(\w+)\s*:")
        combined = []
        for relative_path in source_paths:
            source = read(f"infra/lettuce/{relative_path}")
            declarations = list(declaration_pattern.finditer(source))
            self.assertTrue(declarations, relative_path)
            for declaration in declarations:
                prefix = source[: declaration.start()].rstrip()
                self.assertTrue(prefix.endswith("*/"), f"{relative_path}:{declaration.start()}")
                self.assertEqual(prefix.rfind("/**"), prefix.rfind("/*"))
                if declaration.group(0).lstrip().startswith("data class"):
                    opening = source.find("(", declaration.start())
                    closing = self.matching_parenthesis(source, opening)
                    header = source[opening + 1 : closing]
                    kdoc = prefix[prefix.rfind("/**") :]
                    for property_match in property_pattern.finditer(header):
                        self.assertIn(f"@property {property_match.group(1)}", kdoc)
            combined.append(source)
        for fragment in self.REQUIRED_KDOC_FRAGMENTS:
            self.assertIn(fragment, "\n".join(combined))

    @staticmethod
    def matching_parenthesis(source: str, opening: int) -> int:
        depth = 0
        for index in range(opening, len(source)):
            if source[index] == "(":
                depth += 1
            elif source[index] == ")":
                depth -= 1
                if depth == 0:
                    return index
        raise AssertionError(f"닫히지 않은 생성자 괄호: {opening}")

    @staticmethod
    def code_block(document: str, language: str) -> str:
        opening = f"```{language}"
        start = document.index(opening) + len(opening)
        return document[start : document.index("```", start)].strip()


if __name__ == "__main__":
    unittest.main()
