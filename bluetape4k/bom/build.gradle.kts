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
    return project.findProperty(propertyKey) as? String ?: System.getenv(envKey) ?: ""
}

val gprUser: String = getEnvOrProjectProperty("gpr.user", "GITHUB_USERNAME")
val gprKey: String = getEnvOrProjectProperty("gpr.key", "GITHUB_TOKEN")
val gprPublishKey: String = getEnvOrProjectProperty("gpr.publish.key", "GITHUB_PUBLISH_TOKEN")

publishing {
    publications {
        register("Maven", MavenPublication::class) {
            from(components["javaPlatform"])
        }
    }
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/bluetape4k/bluetape4k-projects")
            credentials {
                username = gprUser
                password = gprPublishKey
            }
        }
        mavenLocal()
    }
}
