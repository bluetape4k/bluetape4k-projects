plugins {
    kotlin("plugin.spring")
    kotlin("plugin.noarg")
    id("com.google.cloud.tools.jib") version "3.4.4"
    id("io.gatling.gradle") version "3.15.0"
}

// Java 25 toolchain — WebFlux uses Netty+Coroutines (not Virtual Threads), but same JVM target as mock-web-server
java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
kotlin { jvmToolchain(25) }
tasks.withType<JavaCompile>().configureEach { options.release.set(25) }
tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
}

// Application module: Jib image build, publishing disabled
tasks.withType<AbstractPublishToMaven>().configureEach { enabled = false }

dependencies {
    // Spring Boot 4 BOM via platform() — KGP 2.3 compatible
    implementation(platform(Libs.spring_boot4_dependencies))
    // Jackson 3 BOM — SB4 does not auto-opt-in
    implementation(platform(Libs.jackson3_bom))

    implementation(Libs.springBootStarter("webflux"))
    implementation(Libs.springBootStarter("cache"))
    implementation(Libs.springBootStarter("actuator"))
    implementation(Libs.caffeine)
    implementation(Libs.jackson3_module_kotlin)

    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)

    implementation(project(":bluetape4k-core"))
    implementation(project(":bluetape4k-coroutines"))
    implementation(project(":bluetape4k-logging"))
    implementation(project(":bluetape4k-jackson2"))

    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
    // Spring Boot 4: WebTestClient 자동구성이 별도 아티팩트(spring-boot-webtestclient)로 분리됨
    testImplementation("org.springframework.boot:spring-boot-webtestclient")
    testImplementation(Libs.kluent)
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(project(":bluetape4k-junit5"))
}

dependencies {
    "gatlingImplementation"("io.gatling.highcharts:gatling-charts-highcharts:3.15.0")
    "gatlingImplementation"("io.gatling:gatling-core-java:3.15.0")
    "gatlingImplementation"("io.gatling:gatling-http-java:3.15.0")
}

afterEvaluate {
    tasks.findByName("gatlingClasses")?.let { gatlingTask ->
        tasks.named("check") {
            setDependsOn(dependsOn.filter { dep ->
                dep.toString() != gatlingTask.path && dep.toString() != "gatlingClasses"
            })
        }
    }
}

tasks.withType<com.google.cloud.tools.jib.gradle.BuildDockerTask>().configureEach {
    notCompatibleWithConfigurationCache("Jib does not support Gradle configuration cache")
    doFirst {
        println(
            """
            ⚠️  jibDockerBuild 는 Gradle Configuration Cache 와 호환되지 않습니다.
               실행 시 반드시 --no-configuration-cache 플래그를 사용하세요:
               ./gradlew :bluetape4k-mock-webflux-server:jibDockerBuild --no-configuration-cache
        """.trimIndent()
        )
    }
}
tasks.withType<com.google.cloud.tools.jib.gradle.BuildImageTask>().configureEach {
    notCompatibleWithConfigurationCache("Jib does not support Gradle configuration cache")
}

val jibMultiPlatform = project.hasProperty("jibMultiPlatform")
val hostArch = when (System.getProperty("os.arch")) {
    "aarch64" -> "arm64"
    else      -> "amd64"
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
        image = "bluetape4k/mock-webflux-server"
        tags = setOf("latest", project.version.toString())
    }
    container {
        ports = listOf("9999")
        jvmFlags = listOf("-XX:+UseG1GC", "-Xmx512m")
        mainClass = "io.bluetape4k.mockwebflux.MockWebfluxServerApplicationKt"
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
