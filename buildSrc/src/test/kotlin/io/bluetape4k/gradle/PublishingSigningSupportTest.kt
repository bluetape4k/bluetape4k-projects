package io.bluetape4k.gradle

import groovy.util.Node
import groovy.xml.XmlParser
import java.util.Base64
import org.gradle.api.GradleException
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PublishingSigningSupportTest {

    @Test
    fun `resolveSigningKeyId keeps short key ID`() {
        assertEquals("5C6DF399", resolveSigningKeyId("5C6DF399"))
    }

    @Test
    fun `resolveSigningKeyId converts long key ID to short key ID`() {
        assertEquals("5C6DF399", resolveSigningKeyId("7CF28E155C6DF399"))
    }

    @Test
    fun `resolveSigningKeyId converts prefixed long key ID to short key ID`() {
        assertEquals("0x5C6DF399", resolveSigningKeyId("0x7CF28E155C6DF399"))
    }

    @Test
    fun `normalizeSigningKeyId returns warning for long key ID`() {
        val normalized = normalizeSigningKeyId("7CF28E155C6DF399")

        assertEquals("5C6DF399", normalized.value)
        assertEquals(
            "Signing key ID used a 16-digit hexadecimal form; normalized to the trailing 8 digits.",
            normalized.warning,
        )
    }

    @Test
    fun `normalizeSigningKeyId keeps short key ID without warning`() {
        val normalized = normalizeSigningKeyId("5C6DF399")

        assertEquals("5C6DF399", normalized.value)
        assertNull(normalized.warning)
    }

    @Test
    fun `resolveSigningKey decodes escaped private key armor`() {
        val privateKey = privateKeyArmor()

        assertEquals(privateKey, resolveSigningKey(privateKey.replace("\n", "\\n")))
    }

    @Test
    fun `resolveSigningKey decodes base64 private key armor`() {
        val privateKey = privateKeyArmor()
        val encoded = Base64.getEncoder().encodeToString(privateKey.toByteArray())

        assertEquals(privateKey, resolveSigningKey(encoded))
    }

    @Test
    fun `resolveSigningKey preserves raw private key armor`() {
        val privateKey = privateKeyArmor()

        assertEquals(privateKey, resolveSigningKey(privateKey))
    }

    @Test
    fun `resolvePublishingSigningConfig preserves gpg fallback and warning`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.extraProperties.set("signingKeyId", "7CF28E155C6DF399")
        project.extensions.extraProperties.set("signingUseGpgCmd", "true")
        project.extensions.extraProperties.set("signing.gnupg.executable", "/custom/bin/gpg")
        project.extensions.extraProperties.set("signing.gnupg.keyName", "release-key")

        val config = project.resolvePublishingSigningConfig()

        assertEquals("5C6DF399", config.keyId)
        assertTrue(config.useGpgCmd)
        assertEquals("/custom/bin/gpg", config.gpgExecutable)
        assertEquals("release-key", config.gpgKeyName)
        assertNotNull(config.keyIdWarning)
    }

    @Test
    fun `configurePublishingSigning applies gpg executable and normalized key fallback`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("signing")
        val executable = project.projectDir.resolve("gpg-fixture").apply {
            writeText("fixture")
        }
        project.extensions.extraProperties.set("signingKeyId", "7CF28E155C6DF399")
        project.extensions.extraProperties.set("signingUseGpgCmd", "true")
        project.extensions.extraProperties.set("signing.gnupg.executable", executable.absolutePath)

        project.configurePublishingSigning("missing-publication")

        assertEquals(
            executable.absolutePath,
            project.extensions.extraProperties.get("signing.gnupg.executable"),
        )
        assertEquals(
            "5C6DF399",
            project.extensions.extraProperties.get("signing.gnupg.keyName"),
        )
    }

    @Test
    fun `동일한 dependencyManagement 항목은 하나만 남긴다`() {
        val pom = pomWithNettyBom("4.1.139.Final", "4.1.139.Final")

        normalizeDependencyManagementDuplicates(pom)

        assertEquals(1, managedDependencies(pom).size)
    }

    @Test
    fun `같은 관리 좌표의 서로 다른 버전은 명시적으로 실패한다`() {
        val pom = pomWithNettyBom("4.1.139.Final", "4.2.19.Final")

        val failure = assertFailsWith<GradleException> {
            normalizeDependencyManagementDuplicates(pom)
        }

        assertTrue(failure.message.orEmpty().contains("io.netty:netty-bom@pom"))
        assertTrue(failure.message.orEmpty().contains("4.1.139.Final"))
        assertTrue(failure.message.orEmpty().contains("4.2.19.Final"))
    }

    @Test
    fun `최종 GenerateMavenPom 산출물에서 동일한 관리 항목을 제거한다`() {
        val destination = generatePomWithNettyBoms("4.1.139.Final", "4.1.139.Final")
        try {
            val pom = XmlParser(false, false).parse(destination)

            assertEquals(1, managedDependencies(pom).size)
        } finally {
            assertTrue(destination.delete(), "합성 POM을 정리해야 한다")
        }
    }

    @Test
    fun `최종 GenerateMavenPom 산출물의 충돌하는 관리 항목을 거부한다`() {
        val failure = assertFailsWith<GradleException> {
            generatePomWithNettyBoms("4.1.139.Final", "4.2.19.Final")
        }

        assertTrue(failure.message.orEmpty().contains("io.netty:netty-bom@pom"))
    }

    @Test
    fun `필요한 Base64 padding을 모두 제공하면 armor로 디코딩한다`() {
        for (padding in listOf("=", "==")) {
            val armor = (0..2).map { privateKeyArmor() + "\n".repeat(it) }
                .first { Base64.getEncoder().encodeToString(it.toByteArray()).takeLastWhile { it == '=' } == padding }
            val encoded = Base64.getEncoder().encodeToString(armor.toByteArray())

            assertEquals(armor, resolveSigningKey(encoded))
            assertEquals(armor, resolveSigningKey("  $encoded\n"))
        }
    }

    @Test
    fun `필요한 Base64 padding이 누락되면 원본 입력을 보존한다`() {
        for (padding in listOf("=", "==")) {
            val armor = (0..2).map { privateKeyArmor() + "\n".repeat(it) }
                .first { Base64.getEncoder().encodeToString(it.toByteArray()).takeLastWhile { it == '=' } == padding }
            val unpadded = Base64.getEncoder().encodeToString(armor.toByteArray()).trimEnd('=')

            assertEquals(unpadded, resolveSigningKey(unpadded))
            assertEquals("  $unpadded\n", resolveSigningKey("  $unpadded\n"))
        }
    }

    @Test
    fun `원본 바이트가 세 배수이면 padding 없는 완전한 Base64를 허용한다`() {
        val armor = (0..2).map { privateKeyArmor() + "\n".repeat(it) }
            .first { it.toByteArray().size % 3 == 0 }
        val encoded = Base64.getEncoder().encodeToString(armor.toByteArray())

        assertTrue(!encoded.endsWith("="))
        assertEquals(armor, resolveSigningKey(encoded))
    }

    @Test
    fun `잘못된 Base64와 armor가 아닌 디코딩 결과는 원본을 보존한다`() {
        val invalidUtf8 = Base64.getEncoder().encodeToString(byteArrayOf(0xC3.toByte(), 0x28))
        for (raw in listOf("not-base64!", "Zm9v", "Zg=", "Zg===", invalidUtf8)) {
            assertEquals(raw, resolveSigningKey(raw))
        }
    }

    private fun privateKeyArmor(): String =
        "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
            "Version: test\n\n" +
            "ZmFrZS1rZXk=\n" +
            "=abcd\n" +
            "-----END PGP PRIVATE KEY BLOCK-----\n"

    private fun pomWithNettyBom(vararg versions: String): Node {
        val pom = Node(null, "project")
        val dependencyManagement = Node(pom, "dependencyManagement")
        val dependencies = Node(dependencyManagement, "dependencies")
        versions.forEach { version ->
            val dependency = Node(dependencies, "dependency")
            Node(dependency, "groupId", "io.netty")
            Node(dependency, "artifactId", "netty-bom")
            Node(dependency, "version", version)
            Node(dependency, "type", "pom")
            Node(dependency, "scope", "import")
        }
        return pom
    }

    private fun generatePomWithNettyBoms(vararg versions: String): java.io.File {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("signing")
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        val publication = publishing.publications.create("test", MavenPublication::class.java)
        publication.groupId = "io.bluetape4k.fixture"
        publication.artifactId = "publication"
        publication.version = "1.0"
        publication.pom.withXml {
            val root = asNode()
            val dependencyManagement = Node(root, "dependencyManagement")
            val dependencies = Node(dependencyManagement, "dependencies")
            versions.forEach { version ->
                val dependency = Node(dependencies, "dependency")
                Node(dependency, "groupId", "io.netty")
                Node(dependency, "artifactId", "netty-bom")
                Node(dependency, "version", version)
                Node(dependency, "type", "pom")
                Node(dependency, "scope", "import")
            }
        }
        project.configurePublishingSigning("test")

        val destination = java.io.File.createTempFile("publishing-pom-", ".xml")
        try {
            val task = project.tasks.getByName("generatePomFileForTestPublication") as GenerateMavenPom
            task.destination = destination
            task.actions.forEach { action -> action.execute(task) }
            return destination
        } catch (failure: Throwable) {
            destination.delete()
            throw failure
        }
    }

    private fun managedDependencies(pom: Node): List<Node> =
        pom.children()
            .filterIsInstance<Node>()
            .first { it.name().toString() == "dependencyManagement" }
            .children()
            .filterIsInstance<Node>()
            .first { it.name().toString() == "dependencies" }
            .children()
            .filterIsInstance<Node>()
            .filter { it.name().toString() == "dependency" }
}
