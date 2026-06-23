package io.bluetape4k.mockwebflux

import io.bluetape4k.assertions.shouldBeFalse
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * README endpoint rows must describe implemented mock-webflux routes only.
 */
class ReadmeRouteContractTest {

    @Test
    fun `README endpoint tables do not list unimplemented mock-webflux routes`() {
        val staleRoutes = listOf(
            "/admin/info",
            "/httpbin/stream-bytes/{n}",
            "/httpbin/drip",
            "/httpbin/sse",
            "/httpbin/brotli",
            "/httpbin/html",
            "/httpbin/xml",
            "/httpbin/json",
            "/httpbin/robots.txt",
            "/httpbin/deny",
        )

        readmeFiles().forEach { readme ->
            val content = readme.readText()

            staleRoutes.forEach { route ->
                content.contains(route).shouldBeFalse()
            }
        }
    }

    private fun readmeFiles(): List<Path> {
        val candidates = listOf(
            Path.of("README.md"),
            Path.of("README.ko.md"),
            Path.of("testing/mock-webflux-server/README.md"),
            Path.of("testing/mock-webflux-server/README.ko.md"),
        ).filter { it.exists() }

        check(candidates.isNotEmpty()) {
            "mock-webflux README files were not found from ${Path.of("").toAbsolutePath()}"
        }

        return candidates.filter { Files.isRegularFile(it) }
    }
}
