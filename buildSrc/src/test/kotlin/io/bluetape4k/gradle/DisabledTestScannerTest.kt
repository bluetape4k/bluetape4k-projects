package io.bluetape4k.gradle

import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DisabledTestScannerTest {

    @Test
    fun `known bug disabled test without issue is reported as violation`() {
        val root = createTempDirectory("disabled-test-scan")
        val source = root.resolve("io/http/src/test/kotlin/sample/KnownBugTest.kt")
        source.parent.createDirectories()
        source.writeText(
            """
            import org.junit.jupiter.api.Disabled
            import org.junit.jupiter.api.Test

            class KnownBugTest {
                @Test
                @Disabled("fails until retry race is fixed")
                fun `retries delayed request`() {
                }
            }
            """.trimIndent(),
        )

        val result = DisabledTestScanner.scan(root.toFile())

        assertEquals(1, result.entries.size)
        assertEquals("known-bug", result.entries.single().category)
        assertEquals(1, result.violations.size)
    }

    @Test
    fun `known bug disabled test with issue reference passes gate`() {
        val root = createTempDirectory("disabled-test-scan")
        val source = root.resolve("cache/cache-core/src/test/kotlin/sample/CacheTest.kt")
        source.parent.createDirectories()
        source.writeText(
            """
            import org.junit.jupiter.api.Disabled
            import org.junit.jupiter.api.Test

            class CacheTest {
                @Disabled("#497 — flaky refresh failure has a tracking issue")
                @Test
                fun `refreshes once`() {
                }
            }
            """.trimIndent(),
        )

        val result = DisabledTestScanner.scan(root.toFile())

        assertEquals(1, result.entries.size)
        assertEquals("known-bug", result.entries.single().category)
        assertEquals("#497", result.entries.single().trackingIssue)
        assertTrue(result.violations.isEmpty())
    }

    @Test
    fun `unsupported and conditional disabled tests are categorized without violations`() {
        val root = createTempDirectory("disabled-test-scan")
        val source = root.resolve("testing/testcontainers/src/test/kotlin/sample/ServerTest.kt")
        source.parent.createDirectories()
        source.writeText(
            """
            import org.junit.jupiter.api.Disabled
            import org.junit.jupiter.api.Test
            import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable

            class ServerTest {
                @Disabled("MiniStack does not support CreateGrant")
                @Test
                fun `creates grant`() {
                }

                @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
                @Test
                fun `streams with external server`() {
                }
            }
            """.trimIndent(),
        )

        val result = DisabledTestScanner.scan(root.toFile())

        assertEquals(listOf("conditional-environment", "unsupported-capability"), result.entries.map { it.category }.sorted())
        assertTrue(result.violations.isEmpty())
    }
}
