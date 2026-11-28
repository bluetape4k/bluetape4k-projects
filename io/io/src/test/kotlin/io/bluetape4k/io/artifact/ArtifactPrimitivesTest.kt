package io.bluetape4k.io.artifact

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

internal class ArtifactPrimitivesTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `trusted paths reject traversal and descendant symlinks`() {
        val root = TrustedArtifactPaths.ensureTrustedDirectory(tempDir.resolve("artifacts"))
        TrustedArtifactPaths.requireIdentifier("run-01", "runId") shouldBeEqualTo "run-01"

        val outside = Files.createDirectory(tempDir.resolve("outside"))
        Files.createSymbolicLink(root.resolve("linked"), outside)

        val failure = assertFailsWith<ArtifactPrimitiveException> {
            TrustedArtifactPaths.resolveCreateTarget(root, Path.of("linked", "report.json"))
        }
        failure.message.shouldNotContain("secret")
        assertFailsWith<IllegalArgumentException> {
            TrustedArtifactPaths.resolveCreateTarget(root, Path.of("..", "escape.json"))
        }
    }

    @Test
    fun `atomic writer publishes exact bytes without replacing a target`() {
        val root = TrustedArtifactPaths.createNewTrustedDirectory(tempDir, "run-01")
        val bytes = "terminal-report\n".toByteArray()
        val target = AtomicArtifactWriter.writeNoReplace(root, Path.of("reports", "terminal.json"), bytes)

        Files.readAllBytes(target).toList() shouldBeEqualTo bytes.toList()
        val failure = assertFailsWith<ArtifactPrimitiveException> {
            AtomicArtifactWriter.writeNoReplace(root, Path.of("reports", "terminal.json"), "replacement".toByteArray())
        }
        failure.message.shouldContain("target already exists")
        Files.readAllBytes(target).toList() shouldBeEqualTo bytes.toList()
    }
}
