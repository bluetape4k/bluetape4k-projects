package io.bluetape4k.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.Base64
import java.util.Properties
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 실제 배포 키 대신 임시 키로 Projects 서명 어댑터의 산출물을 검증한다. */
class PublishingSigningIntegrationTest {

    @Test
    fun `메모리 키와 GPG fallback으로 실제 publication을 서명한다`() {
        withFixture { fixture ->
            fixture.generateKey()
            val armor = fixture.exportKey()
            val encoded = Base64.getEncoder().encodeToString(armor.toByteArray())
            for ((name, key) in listOf("armor" to armor, "base64" to encoded, "gpg" to "")) {
                val project = fixture.project(name, key, useGpg = name == "gpg")
                val result = fixture.runner(project).build()

                assertEquals(TaskOutcome.SUCCESS, result.task(":signReleasePublication")?.outcome)
                fixture.assertNoSecrets(result.output, armor, encoded)
                val artifact = project.resolve("payload.txt")
                val signature = project.resolve("payload.txt.asc")
                assertTrue(signature.isFile, "publication 서명 파일이 필요하다")
                fixture.verify(signature, artifact)

                artifact.appendText("변조")
                fixture.verify(signature, artifact, expectedSuccess = false)
            }
        }
    }

    @Test
    fun `잘못된 private key는 서명에 실패하며 비밀 입력을 출력하지 않는다`() {
        withFixture { fixture ->
            val malformed = "malformed-private-key-sentinel"
            val project = fixture.project("malformed", malformed)

            val result = fixture.runner(project).buildAndFail()

            fixture.assertNoSecrets(result.output, malformed, "malformed-private-key-sentinel")
            assertFalse(project.resolve("payload.txt.asc").exists())
            assertTrue(result.output.contains("Could not read PGP secret key"), "PGP 키 파싱 실패여야 한다")
        }
    }

    private fun withFixture(block: (SigningFixture) -> Unit) {
        // macOS 기본 임시 경로는 gpg-agent Unix 소켓 경로 제한을 넘을 수 있다.
        val root = createTempDirectory(Path.of("/tmp"), "signing-").toFile()
        try {
            SigningFixture(root).use(block)
        } finally {
            assertTrue(root.deleteRecursively(), "합성 서명 임시 파일을 정리해야 한다")
        }
    }

    private class SigningFixture(private val root: File): AutoCloseable {
        private val home = root.resolve("gnupg").apply {
            mkdirs()
            Files.setPosixFilePermissions(toPath(), PosixFilePermissions.fromString("rwx------"))
        }
        private val password = "synthetic-signing-regression-password"
        private val identity = "Signing Fixture <signing-fixture@example.invalid>"
        private val gpg = executable("gpg")
        private val gpgconf = executable("gpgconf")
        private val environment = System.getenv().filterKeys {
            !it.startsWith("SIGNING_") && !it.startsWith("GPG_") &&
                !it.startsWith("ORG_GRADLE_PROJECT_") && it != "GNUPGHOME" &&
                it != "GRADLE_USER_HOME" && it != "GRADLE_OPTS" && it != "JAVA_TOOL_OPTIONS"
        } + mapOf("GNUPGHOME" to home.absolutePath)

        fun generateKey() {
            command(gpg, "--batch", "--pinentry-mode", "loopback", "--passphrase-fd", "0",
                "--quick-generate-key", identity, "rsa2048", "sign", "0", input = "$password\n")
        }

        fun exportKey(): String =
            command(gpg, "--batch", "--pinentry-mode", "loopback", "--passphrase-fd", "0",
                "--armor", "--export-secret-keys", identity, input = "$password\n")

        fun project(name: String, key: String, useGpg: Boolean = false): File {
            val dir = root.resolve(name).apply { mkdirs() }
            dir.resolve("settings.gradle").writeText("rootProject.name = 'signing-fixture'\n")
            dir.resolve("payload.txt").writeText("합성 publication 산출물\n")
            val properties = Properties().apply {
                setProperty("signingKey", key)
                setProperty("signingKeyId", "")
                setProperty("signingPassword", if (useGpg) "" else password)
                setProperty("signingUseGpgCmd", useGpg.toString())
                setProperty("signing.gnupg.executable", gpg)
                setProperty("signing.gnupg.keyName", identity)
                setProperty("signing.gnupg.homeDir", home.absolutePath)
                setProperty("signing.gnupg.passphrase", password)
            }
            dir.resolve("signing-fixture.properties").outputStream().use { properties.store(it, null) }
            // 복사한 helper가 아니라 현재 buildSrc가 컴파일한 어댑터와 helper를 로드한다.
            val classpath = listOf(PublishingSigningConfig::class.java, Unit::class.java)
                .map { File(it.protectionDomain.codeSource.location.toURI()).absolutePath }
                .joinToString(", ") { "'" + it.replace("\\", "\\\\").replace("'", "\\'") + "'" }
            dir.resolve("build.gradle").writeText(
                """
                buildscript { dependencies { classpath files($classpath) } }
                apply plugin: 'maven-publish'
                apply plugin: 'signing'
                group = 'io.bluetape4k.fixture'
                version = '1.0'
                def inputs = new Properties()
                file('signing-fixture.properties').withInputStream { inputs.load(it) }
                inputs.each { key, value -> project.ext.set(key, value) }
                publishing {
                    publications {
                        release(MavenPublication) { artifact(file('payload.txt')) }
                    }
                }
                io.bluetape4k.gradle.PublishingSigningSupportKt.configurePublishingSigning(
                    project, 'release', true, '서명 키가 필요합니다')
                """.trimIndent() + "\n",
            )
            return dir
        }

        fun runner(project: File): GradleRunner = GradleRunner.create()
            .withProjectDir(project)
            .withTestKitDir(root.resolve("testkit"))
            .withEnvironment(environment)
            .withArguments("signReleasePublication", "--offline", "--no-configuration-cache",
                "--no-build-cache", "--max-workers=1", "--console=plain", "--stacktrace")

        fun verify(signature: File, artifact: File, expectedSuccess: Boolean = true) {
            command(gpg, "--batch", "--verify", signature.absolutePath, artifact.absolutePath,
                expectedSuccess = expectedSuccess)
        }

        fun assertNoSecrets(output: String, vararg secrets: String) {
            val secretLines = secrets.flatMap { it.lines() }.filter { it.length >= 24 }
            for (secret in listOf(password, "-----BEGIN PGP PRIVATE KEY BLOCK-----") + secrets + secretLines) {
                assertFalse(output.contains(secret), "서명 출력에 비밀 입력이 노출되어서는 안 된다")
            }
        }

        private fun command(vararg arguments: String, input: String = "", expectedSuccess: Boolean = true): String {
            val output = File.createTempFile("command-", ".log", root)
            val process = ProcessBuilder(*arguments).apply {
                environment().clear()
                environment().putAll(environment)
                redirectErrorStream(true)
                redirectOutput(output)
            }.start()
            try {
                process.outputStream.bufferedWriter().use { it.write(input) }
                assertTrue(process.waitFor(60, TimeUnit.SECONDS), "합성 GPG 작업이 제한 시간 안에 끝나야 한다")
                assertEquals(expectedSuccess, process.exitValue() == 0, "합성 GPG 작업의 종료 상태가 예상과 다르다")
                return output.readText()
            } finally {
                if (process.isAlive) {
                    process.destroyForcibly()
                    process.waitFor(10, TimeUnit.SECONDS)
                }
                output.delete()
            }
        }

        override fun close() {
            command(gpgconf, "--homedir", home.absolutePath, "--kill", "gpg-agent")
        }

        private fun executable(name: String): String =
            System.getenv("PATH").orEmpty().split(File.pathSeparator)
                .map { File(it, name) }.firstOrNull { it.isFile && it.canExecute() }?.absolutePath
                ?: error("서명 회귀 검증에 $name 실행 파일이 필요합니다")
    }
}
