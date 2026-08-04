plugins {
    kotlin("plugin.spring")
    kotlin("plugin.noarg")
    // Spring Boot plugin은 buildscript 클래스패스에 이미 Spring Boot 3.x가 있어 버전 충돌 발생.
    // BOM(platform)으로 Spring Boot 4 의존성을 관리하고, Jib으로 직접 컨테이너 이미지 생성.
    alias(bt4k.plugins.jib)
    alias(bt4k.plugins.gatling)
}

// Java 21 toolchain (workspace baseline)
java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
kotlin { jvmToolchain(21) }
tasks.withType<JavaCompile>().configureEach { options.release.set(21) }
tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}

// Spring Boot Application 모듈: fat-jar 빌드, publishing 불필요
tasks.withType<AbstractPublishToMaven>().configureEach { enabled = false }

dependencies {
    // Spring Boot 4 BOM: platform() 방식 필수 (dependencyManagement 사용 금지 - KGP 2.3 충돌)
    implementation(platform(bt4k.spring.boot4.dependencies))
    // Jackson 3 BOM: Spring Boot 4는 tools.jackson.* (Jackson 3) 사용
    implementation(platform("tools.jackson:jackson-bom:${bt4k.versions.jackson3.get()}"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation(libs.caffeine)
    implementation(libs.jackson3.module.kotlin)

    implementation(project(":bluetape4k-core"))
    implementation(project(":bluetape4k-logging"))
    implementation(project(":bluetape4k-jackson3"))

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
    testImplementation(libs.okhttp3)
    testImplementation(project(":bluetape4k-junit5"))
}

// Jib 플러그인은 Gradle Configuration Cache와 호환되지 않음을 선언합니다.
// 이 선언이 없으면 configuration cache 활성화 시 "this.project is null" 오류가 발생합니다.
tasks.withType<com.google.cloud.tools.jib.gradle.BuildDockerTask>().configureEach {
    notCompatibleWithConfigurationCache("Jib does not support Gradle configuration cache")
    doFirst {
        println(
            """
            ⚠️  jibDockerBuild 는 Gradle Configuration Cache 와 호환되지 않습니다.
               실행 시 반드시 --no-configuration-cache 플래그를 사용하세요:
               ./gradlew :bluetape4k-mock-web-server:jibDockerBuild --no-configuration-cache
        """.trimIndent()
        )
    }
}
tasks.withType<com.google.cloud.tools.jib.gradle.BuildImageTask>().configureEach {
    notCompatibleWithConfigurationCache("Jib does not support Gradle configuration cache")
}

// 멀티 플랫폼 여부: -PjibMultiPlatform=true 로 활성화 (CI/CD registry push 전용)
val jibMultiPlatform = project.hasProperty("jibMultiPlatform")

// 호스트 아키텍처 감지: aarch64 = arm64 (Apple Silicon), 그 외 = amd64
val hostArch = when (System.getProperty("os.arch")) {
    "aarch64" -> "arm64"
    else -> "amd64"
}

jib {
    from {
        image = "eclipse-temurin:21-jre-alpine"
        platforms {
            if (jibMultiPlatform) {
                platform { architecture = "amd64"; os = "linux" }
                platform { architecture = "arm64"; os = "linux" }
            } else {
                platform { architecture = hostArch; os = "linux" }
            }
        }
    }
    to {
        image = "bluetape4k/mock-web-server"
        tags = setOf("latest", project.version.toString())
    }
    container {
        ports = listOf("80", "8443")
        jvmFlags = listOf("-XX:+UseG1GC", "-Xmx512m")
        mainClass = "io.bluetape4k.mockserver.MockServerApplicationKt"
    }
    dockerClient {
        val dockerHostEnv = System.getenv("DOCKER_HOST")
        if (dockerHostEnv != null) {
            environment = mapOf("DOCKER_HOST" to dockerHostEnv)
        } else if (System.getProperty("os.name").lowercase().contains("mac")) {
            executable = "/opt/homebrew/bin/docker"
            environment = mapOf("DOCKER_HOST" to "unix:///Users/debop/.colima/default/docker.sock")
        }
        // Linux (CI): default docker + /var/run/docker.sock
    }
}

dependencies {
    add("gatlingImplementation", bt4k.gatling.charts.highcharts)
    add("gatlingImplementation", bt4k.gatling.core.java)
    add("gatlingImplementation", bt4k.gatling.http.java)
}

// Prevent Gatling tasks from running during the standard `check` lifecycle.
afterEvaluate {
    tasks.findByName("gatlingClasses")?.let { gatlingTask ->
        tasks.named("check") {
            setDependsOn(dependsOn.filter { dep ->
                dep.toString() != gatlingTask.path && dep.toString() != "gatlingClasses"
            })
        }
    }
}
