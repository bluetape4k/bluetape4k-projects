plugins {
    `java-platform`
    `maven-publish`
    signing
}

dependencies {
    constraints {
        rootProject.subprojects {
            if (name != "bluetape4k-bom") {
                api(this)
            }
        }
    }
}

// NOTE: .zshrc 에 정의하거나, ~/.gradle/gradle.properties 에 정의해주세요.
fun getEnvOrProjectProperty(propertyKey: String, envKey: String): String {
    return project.findProperty(propertyKey) as? String ?: System.getenv(envKey).orEmpty()
}


val signingKeyId: String = getEnvOrProjectProperty("signingKeyId", "SIGNING_KEY_ID")
val signingKey: String = getEnvOrProjectProperty("signingKey", "SIGNING_KEY")
    .replace("\\n", "\n")
val signingPassword: String = getEnvOrProjectProperty("signingPassword", "SIGNING_PASSWORD")
val signingUseGpgCmd: Boolean = getEnvOrProjectProperty("signingUseGpgCmd", "SIGNING_USE_GPG_CMD")
    .toBoolean()
val signingGpgExecutable: String = getEnvOrProjectProperty("signing.gnupg.executable", "GPG_EXECUTABLE")
    .ifBlank { "/opt/homebrew/bin/gpg" }
val signingGpgKeyName: String = getEnvOrProjectProperty("signing.gnupg.keyName", "GPG_KEY_NAME")
    .ifBlank { signingKeyId }

publishing {
    publications {
        register("Bluetape4k", MavenPublication::class) {
            from(components["javaPlatform"])
            pom {
                name.set("bluetape4k-bom")
                description.set("BOM for Bluetape4k modules")
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
        }
    }
    repositories {
        mavenCentral()
        google()
        mavenLocal()
    }
}

signing {
    if (signingKey.isNotBlank() && signingPassword.isNotBlank()) {
        useInMemoryPgpKeys(signingKeyId.ifBlank { null }, signingKey, signingPassword)
        sign(publishing.publications["Bluetape4k"])
    } else if (signingUseGpgCmd) {
        if (file(signingGpgExecutable).exists()) {
            project.extensions.extraProperties["signing.gnupg.executable"] = signingGpgExecutable
        }
        if (signingGpgKeyName.isNotBlank()) {
            project.extensions.extraProperties["signing.gnupg.keyName"] = signingGpgKeyName
        }
        useGpgCmd()
        sign(publishing.publications["Bluetape4k"])
    } else if (signingPassword.isNotBlank()) {
        logger.warn(
            "서명 키가 없어 서명을 수행하지 않습니다. " +
                    "SIGNING_KEY(+SIGNING_PASSWORD)를 우선 설정하고, 필요 시 SIGNING_USE_GPG_CMD=true를 사용하세요."
        )
    }
}
