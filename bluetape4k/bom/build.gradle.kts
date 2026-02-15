plugins {
    `java-platform`
    `maven-publish`
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

publishing {
    publications {
        register("Bluetape4k", MavenPublication::class) {
            from(components["javaPlatform"])
        }
    }
    repositories {
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
