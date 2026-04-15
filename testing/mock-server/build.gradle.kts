plugins {
    kotlin("plugin.spring")
    kotlin("plugin.noarg")
    // Spring Boot plugin은 buildscript 클래스패스에 이미 Spring Boot 3.x가 있어 버전 충돌 발생.
    // BOM(platform)으로 Spring Boot 4 의존성을 관리하고, Jib으로 직접 컨테이너 이미지 생성.
    id("com.google.cloud.tools.jib") version "3.4.4"
}

// Java 25 툴체인 (mock-server 전용 — Spring Boot 4 + Virtual Threads 최적화)
java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
kotlin { jvmToolchain(25) }
tasks.withType<JavaCompile>().configureEach { options.release.set(25) }
tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
}

// Spring Boot Application 모듈: fat-jar 빌드, publishing 불필요
tasks.withType<AbstractPublishToMaven>().configureEach { enabled = false }

dependencies {
    // Spring Boot 4 BOM: platform() 방식 필수 (dependencyManagement 사용 금지 - KGP 2.3 충돌)
    implementation(platform(Libs.spring_boot4_dependencies))
    // Jackson 3 BOM: SB4는 tools.jackson.* (Jackson 3) 사용
    implementation(platform(Libs.jackson3_bom))

    implementation(Libs.springBootStarter("web"))
    implementation(Libs.springBootStarter("cache"))
    implementation(Libs.caffeine)
    implementation(Libs.jackson3_module_kotlin)

    implementation(project(":bluetape4k-core"))
    implementation(project(":bluetape4k-logging"))
    implementation(project(":bluetape4k-jackson2"))

    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
    testImplementation(Libs.kluent)
    testImplementation(Libs.okhttp3)
    testImplementation(project(":bluetape4k-junit5"))
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
        image = "eclipse-temurin:25-jre-alpine"
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
        image = "bluetape4k/mock-server"
        tags = setOf("latest", project.version.toString())
    }
    container {
        ports = listOf("8888")
        jvmFlags = listOf("-XX:+UseG1GC", "-Xmx512m")
        mainClass = "io.bluetape4k.mockserver.MockServerApplicationKt"
    }
    // Docker 실행 파일 경로 (Colima 환경)
    dockerClient {
        executable = "/opt/homebrew/bin/docker"
        environment = mapOf("DOCKER_HOST" to "unix:///Users/debop/.colima/default/docker.sock")
    }
}
