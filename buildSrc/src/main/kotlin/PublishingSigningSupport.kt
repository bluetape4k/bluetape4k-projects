package io.bluetape4k.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.kotlin.dsl.configure
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
)

/**
 * Publishing signing 설정을 project property / 환경 변수에서 로딩합니다.
 */
fun Project.resolvePublishingSigningConfig(): PublishingSigningConfig {
    val keyId = getEnvOrProjectProperty("signingKeyId", "SIGNING_KEY_ID")
    val key = getEnvOrProjectProperty("signingKey", "SIGNING_KEY").replace("\\n", "\n")
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
