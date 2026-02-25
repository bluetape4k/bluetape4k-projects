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

// NOTE: Nexus 에 등록된 것 때문에 사용한다
// NOTE: .zshrc 에 정의하던가, ~/.gradle/gradle.properties 에 정의해주셔야 합니다.
fun getEnvOrProjectProperty(propertyKey: String, envKey: String): String {
    return project.findProperty(propertyKey) as? String ?: System.getenv(envKey).orEmpty()
}

val bluetape4kGprUser: String = getEnvOrProjectProperty("gpr.user", "BLUETAPE4K_GITHUB_USERNAME")
val bluetape4kGprKey: String = getEnvOrProjectProperty("gpr.key", "BLUETAPE4K_GITHUB_TOKEN")
val bluetape4kGprPublishKey: String = getEnvOrProjectProperty("gpr.publish.key", "BLUETAPE4K_GITHUB_PUBLISH_TOKEN")

val centralUser: String = getEnvOrProjectProperty("central.user", "CENTRAL_USERNAME")
val centralPassword: String = getEnvOrProjectProperty("central.password", "CENTRAL_PASSWORD")

val signingPassword: String = getEnvOrProjectProperty("signingPassword", "SIGNING_PASSWORD")
val signingUseGpgCmd: Boolean = getEnvOrProjectProperty("signingUseGpgCmd", "SIGNING_USE_GPG_CMD")
    .toBoolean()

publishing {
    publications {
        register("Bluetape4k", MavenPublication::class) {
            from(components["javaPlatform"])
        }
    }
    repositories {
        maven {
            name = "Central"
            url = uri(
                if (version.toString().endsWith("SNAPSHOT")) {
                    "https://central.sonatype.com/repository/maven-snapshots/"
                } else {
                    "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
                }
            )
            credentials {
                username = centralUser
                password = centralPassword
            }
        }
        maven {
            name = "Bluetape4k"
            url = uri("https://maven.pkg.github.com/bluetape4k/bluetape4k-projects")
            credentials {
                username = bluetape4kGprUser
                password = bluetape4kGprPublishKey
            }
        }
        mavenLocal()
    }
}

signing {
    if (signingUseGpgCmd) {
        useGpgCmd()
        sign(publishing.publications["Bluetape4k"])
    } else if (signingPassword.isNotBlank()) {
        logger.warn("SIGNING_USE_GPG_CMD is false. GPG command signing is disabled.")
    }
}
