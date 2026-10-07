package io.bluetape4k.gradle

import groovy.util.Node
import groovy.xml.XmlParser
import groovy.xml.XmlUtil
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension

/**
 * Project property 또는 환경 변수에서 값을 조회합니다.
 */
fun Project.getEnvOrProjectProperty(propertyKey: String, envKey: String): String {
    return findProperty(propertyKey) as? String ?: System.getenv(envKey).orEmpty()
}

data class CentralPublishingConfig(
    val username: String,
    val password: String,
)

/**
 * Central Portal 자격증명을 project property / 환경 변수에서 로딩합니다.
 */
fun Project.resolveCentralPublishingConfig(): CentralPublishingConfig {
    return CentralPublishingConfig(
        username = getEnvOrProjectProperty("central.user", "CENTRAL_USERNAME"),
        password = getEnvOrProjectProperty("central.password", "CENTRAL_PASSWORD"),
    )
}

/**
 * Central Snapshots 저장소를 공통 규약으로 추가합니다.
 */
fun RepositoryHandler.centralSnapshotsRepository(
    project: Project,
    repositoryName: String = "CentralSnapshots",
    repositoryUrl: String = "https://central.sonatype.com/repository/maven-snapshots/",
) {
    val central = project.resolveCentralPublishingConfig()
    maven {
        name = repositoryName
        url = project.uri(repositoryUrl)
        mavenContent {
            snapshotsOnly()
        }
        credentials {
            username = central.username
            password = central.password
        }
    }
}

/**
 * Bluetape4k 공통 POM 메타데이터를 적용합니다.
 */
fun MavenPom.applyBluetape4kPomMetadata(
    artifactDisplayName: String,
    artifactDescription: String,
) {
    name.set(artifactDisplayName)
    description.set(artifactDescription)
    url.set("https://github.com/bluetape4k/bluetape4k-projects")
    licenses {
        license {
            name.set("The Apache License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
        }
    }
    developers {
        developer {
            id.set("debop")
            name.set("Sunghyouk Bae")
            email.set("sunghyouk.bae@gmail.com")
            organization.set("Bluetape4k")
            organizationUrl.set("https://github.com/bluetape4k")
        }
    }
    scm {
        url.set("https://www.github.com/bluetape4k/bluetape4k-projects")
        connection.set("scm:git:https://www.github.com/bluetape4k/bluetape4k-projects")
        developerConnection.set("scm:git:https://www.github.com/bluetape4k/bluetape4k-projects")
    }
}

data class PublishingSigningConfig(
    val keyId: String,
    val key: String,
    val password: String,
    val useGpgCmd: Boolean,
    val gpgExecutable: String,
    val gpgKeyName: String,
    val keyIdWarning: String?,
)

private data class ManagedDependencyKey(
    val groupId: String,
    val artifactId: String,
    val type: String,
    val classifier: String,
) {
    fun coordinate(): String = buildString {
        append(groupId)
        append(':')
        append(artifactId)
        if (classifier.isNotEmpty()) {
            append(':')
            append(classifier)
        }
        append('@')
        append(type)
    }
}

private data class ManagedDependencyEntry(
    val parent: Node,
    val node: Node,
    val key: ManagedDependencyKey,
    val fingerprint: String,
)

/**
 * 게시 POM의 dependencyManagement에서 동일한 관리 항목을 하나로 합칩니다.
 *
 * Gradle의 여러 BOM/platform 선언은 같은 관리 좌표를 POM에 반복해서 기록할 수
 * 있습니다. 완전히 같은 XML 항목은 첫 항목만 남기지만, 같은 좌표에 서로 다른
 * 버전이나 속성이 있으면 호환성 의도를 추측하지 않고 즉시 실패시킵니다.
 */
fun normalizeDependencyManagementDuplicates(pom: Node) {
    val entries = pom.children()
        .filterIsInstance<Node>()
        .filter { it.elementName() == "dependencyManagement" }
        .flatMap { dependencyManagement ->
            dependencyManagement.children()
                .filterIsInstance<Node>()
                .filter { it.elementName() == "dependencies" }
                .flatMap { dependencies ->
                    dependencies.children()
                        .filterIsInstance<Node>()
                        .filter { it.elementName() == "dependency" }
                        .map { dependency ->
                            ManagedDependencyEntry(
                                parent = dependencies,
                                node = dependency,
                                key = ManagedDependencyKey(
                                    groupId = dependency.childText("groupId"),
                                    artifactId = dependency.childText("artifactId"),
                                    type = dependency.childText("type").ifBlank { "jar" },
                                    classifier = dependency.childText("classifier"),
                                ),
                                fingerprint = dependency.canonicalFingerprint(),
                            )
                        }
                }
        }

    val firstByKey = linkedMapOf<ManagedDependencyKey, ManagedDependencyEntry>()
    val duplicateNodes = mutableListOf<ManagedDependencyEntry>()
    for (entry in entries) {
        val first = firstByKey.putIfAbsent(entry.key, entry)
        when {
            first == null -> Unit
            first.fingerprint == entry.fingerprint -> duplicateNodes += entry
            else -> throw GradleException(
                "Maven POM contains conflicting dependencyManagement entries for " +
                    "${entry.key.coordinate()}: ${first.fingerprint} vs ${entry.fingerprint}",
            )
        }
    }

    duplicateNodes.forEach { duplicate -> duplicate.parent.remove(duplicate.node) }
}

private fun Node.childText(name: String): String =
    children()
        .filterIsInstance<Node>()
        .firstOrNull { it.elementName() == name }
        ?.text()
        ?.trim()
        .orEmpty()

private fun Node.elementName(): String = name().toString().substringAfterLast('}')

private fun Node.canonicalFingerprint(): String = buildString {
    append(elementName())
    val rawAttributes = attributes() as Map<*, *>
    val attributes = rawAttributes.entries
        .sortedBy { it.key.toString() }
        .joinToString(separator = ",") { entry ->
            "${entry.key}=${entry.value.toString().trim()}"
        }
    if (attributes.isNotEmpty()) {
        append("[$attributes]")
    }
    children()
        .mapNotNull { child ->
            when (child) {
                is Node -> child.canonicalFingerprint()
                else -> child?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            }
        }
        .sorted()
        .forEach(::append)
}

/**
 * Publishing signing 설정을 project property / 환경 변수에서 로딩합니다.
 */
fun Project.resolvePublishingSigningConfig(): PublishingSigningConfig {
    val normalizedKeyId = normalizeSigningKeyId(getEnvOrProjectProperty("signingKeyId", "SIGNING_KEY_ID"))
    val keyId = normalizedKeyId.value
    val key = resolveSigningKey(getEnvOrProjectProperty("signingKey", "SIGNING_KEY"))
    val password = getEnvOrProjectProperty("signingPassword", "SIGNING_PASSWORD")
    val useGpgCmd = getEnvOrProjectProperty("signingUseGpgCmd", "SIGNING_USE_GPG_CMD").toBoolean()
    val gpgExecutable = getEnvOrProjectProperty("signing.gnupg.executable", "GPG_EXECUTABLE")
        .ifBlank { "/opt/homebrew/bin/gpg" }
    val gpgKeyName = getEnvOrProjectProperty("signing.gnupg.keyName", "GPG_KEY_NAME")
        .ifBlank { keyId }

    return PublishingSigningConfig(
        keyId = keyId,
        key = key,
        password = password,
        useGpgCmd = useGpgCmd,
        gpgExecutable = gpgExecutable,
        gpgKeyName = gpgKeyName,
        keyIdWarning = normalizedKeyId.warning,
    )
}

/**
 * Maven publication 서명 설정을 공통으로 적용합니다.
 */
fun Project.configurePublishingSigning(
    publicationName: String,
    enabled: Boolean = true,
    missingKeyWarning: String = "서명 키가 없어 서명을 수행하지 않습니다. " +
            "SIGNING_KEY(+SIGNING_PASSWORD)를 우선 설정하고, 필요 시 SIGNING_USE_GPG_CMD=true를 사용하세요.",
) {
    if (!enabled) return

    val config = resolvePublishingSigningConfig()
    config.keyIdWarning?.let(project.logger::warn)

    // dependency-management-plugin contributes managed entries after the
    // MavenPom XML callbacks have run. Normalize the final generated file so
    // every contribution is present before duplicate/conflict validation.
    tasks.withType<GenerateMavenPom>().configureEach {
        doLast {
            val destination = destinationFile.get().asFile
            if (!destination.isFile) return@doLast

            val node = XmlParser(false, false).parse(destination)
            normalizeDependencyManagementDuplicates(node)
            destination.writeText(XmlUtil.serialize(node))
        }
    }

    extensions.configure<SigningExtension> {
        when {
            config.key.isNotBlank() && config.password.isNotBlank() -> {
                useInMemoryPgpKeys(config.keyId.ifBlank { null }, config.key, config.password)
            }

            config.useGpgCmd -> {
                if (file(config.gpgExecutable).exists()) {
                    project.extensions.extraProperties["signing.gnupg.executable"] = config.gpgExecutable
                }
                if (config.gpgKeyName.isNotBlank()) {
                    project.extensions.extraProperties["signing.gnupg.keyName"] = config.gpgKeyName
                }
                useGpgCmd()
            }

            config.password.isNotBlank() -> {
                project.logger.warn(missingKeyWarning)
                return@configure
            }

            else -> return@configure
        }

        val publishing = project.extensions.findByType(PublishingExtension::class.java)
        val publication = publishing?.publications?.findByName(publicationName)
        if (publication != null) {
            sign(publication)
        }
    }
}
