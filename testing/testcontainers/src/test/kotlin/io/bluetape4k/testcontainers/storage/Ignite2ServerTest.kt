package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.apache.ignite.Ignition
import org.apache.ignite.configuration.ClientConfiguration
import org.testcontainers.utility.DockerImageName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import io.bluetape4k.assertions.assertFailsWith
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class Ignite2ServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Test
    @Timeout(value = 30, unit = TimeUnit.MINUTES)
    fun representativeStartupAndWorkload() {
        Ignite2Server().use { server ->
            server.start()
            writeWorkloadEvidence(server)

            @Suppress("DEPRECATION")
            val clientConfiguration = ClientConfiguration()
                .setAddresses(server.url)
                .setTimeout(30_000)
                .setRequestTimeout(30_000)

            Ignition.startClient(clientConfiguration).use { client ->
                val cache = client.getOrCreateCache<String, String>("ignite2-image-gate")
                cache.put("representative-key", "representative-value")
                cache.get("representative-key") shouldBeEqualTo "representative-value"
            }
        }
    }

    private fun writeWorkloadEvidence(server: Ignite2Server) {
        val evidenceDir = System.getProperty("testcontainers.image-gate.evidence-dir") ?: return
        val root = Path.of(evidenceDir).toAbsolutePath().normalize()
        Files.createDirectories(root)
        Files.writeString(root.resolve("startup.marker"), "Ignite node started OK\n")
        Files.writeString(root.resolve("workload.image-id"), "${server.containerInfo.imageId}\n")
    }

    @Test
    fun `create ignite2 server`() {
        Ignite2Server().use {
            it.start()
            it.isRunning.shouldBeTrue()
        }
    }

    @Test
    fun `create ignite2 server with default port`() {
        Ignite2Server(useDefaultPort = true).use {
            it.start()
            it.isRunning.shouldBeTrue()
            it.port shouldBeEqualTo Ignite2Server.PORT
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { Ignite2Server(image = " ") }
        assertFailsWith<IllegalArgumentException> { Ignite2Server(tag = " ") }
    }

    @Test
    fun `default tag follows the native architecture and custom images stay explicit`() {
        val originalArchitecture = System.getProperty("os.arch")
        try {
            System.setProperty("os.arch", "x86_64")
            Ignite2Server.DEFAULT_TAG shouldBeEqualTo Ignite2Server.TAG

            System.setProperty("os.arch", "arm64")
            Ignite2Server.DEFAULT_TAG shouldBeEqualTo "${Ignite2Server.TAG}-arm64"
            assertFailsWith<IllegalArgumentException> { Ignite2Server(image = "custom/ignite") }
            Ignite2Server(image = "custom/ignite", tag = "2.18.0-custom").use { }

            System.setProperty("os.arch", "mips64")
            assertFailsWith<IllegalStateException> { Ignite2Server() }
        } finally {
            if (originalArchitecture == null) {
                System.clearProperty("os.arch")
            } else {
                System.setProperty("os.arch", originalArchitecture)
            }
        }
    }

    @Test
    fun `DockerImageName tagless canonical image resolves while custom tagless image fails`() {
        val canonical = Ignite2Server(DockerImageName.parse(Ignite2Server.IMAGE))
        canonical.dockerImageName shouldBeEqualTo
            "${Ignite2Server.IMAGE}:${Ignite2Server.DEFAULT_TAG}"
        assertFailsWith<IllegalArgumentException> {
            Ignite2Server(DockerImageName.parse("custom/ignite"))
        }
    }
}
