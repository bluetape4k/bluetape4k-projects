plugins {
    kotlin("plugin.spring")
    alias(libs.plugins.shadow)

    id("de.undercouch.download") version "5.6.0"
}

// OpenTelemetry Java Agent 를 사용할 경우 아래의 Task 를 실행하여 자동으로 다운로드 하도록 합니다.
tasks {
    test {
        // dependsOn("downloadAgent")
        // jvmArgs = listOf("-javaagent:${project.layout.projectDirectory.asFile}/opentelemetry-javaagent.jar")

        // io_uring 이벤트 루프 재초기화 시 발생하는 레이스 컨디션 방지 (eventfd_write: Bad file descriptor)
        jvmArgs("-Dreactor.netty.native=false")
    }

    // update-otel-agent.sh 를 수동으로 실행하여 OpenTelemetry Java Agent 를 다운로드 받을 수 있습니다.
    // Download the OpenTelemetry java agent and put it in the build directory
    //    task<Download>("downloadAgent") {
    //        src(libs.opentelemetry.javaagent.remote.path)
    //        dest("${project.layout.buildDirectory.asFile.get()}/${libs.opentelemetry.javaagent.local.path}")
    //        onlyIfModified(true)
    //        onlyIfNewer(true)
    //        download()
    //    }
}

configurations {
    // compileOnly 나 runtimeOnly로 지정된 Dependency를 testImplementation 으로도 지정하도록 합니다.
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencyManagement {
    imports {
        mavenBom(libs.opentelemetry.bom.get().toString())
        mavenBom(libs.opentelemetry.alpha.bom.get().toString())
        mavenBom(libs.opentelemetry.instrumentation.bom.alpha.get().toString())
    }
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    api(project(":bluetape4k-io"))
    implementation(project(":bluetape4k-netty"))
    testImplementation(project(":bluetape4k-junit5"))

    // OpenTelemetry
    api(libs.opentelemetry.api)
    api(libs.opentelemetry.sdk)
    api(libs.opentelemetry.extension.kotlin)
    compileOnly(libs.opentelemetry.sdk.extensions.autoconfigure)
    compileOnly(libs.opentelemetry.sdk.metrics)
    compileOnly(libs.opentelemetry.sdk.logs)
    compileOnly(libs.opentelemetry.sdk.trace)
    compileOnly(libs.opentelemetry.sdk.testing)

    compileOnly(libs.opentelemetry.exporter.logging)
    // logback mdc 로 otel 정보를 전달하는 라이브러리
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/logback/logback-mdc-1.0/library
    testRuntimeOnly(libs.opentelemetry.logback.mdc)
    // otel 이 jul 을 사용해서 로그를 남기는데, 이를 slf4j 로 전달해주는 라이브러리
    compileOnly(libs.jul.to.slf4j)

    // Opentelemetry instrumentation for spring boot starter
    implementation(libs.opentelemetry.spring.boot.starter)

    // Spring WebFlux tracing support (compileOnly — classpath 미존재 시 다른 기능에 영향 없도록)
    compileOnly(libs.opentelemetry.spring.webflux)
    compileOnly("org.springframework.boot:spring-boot-starter-webflux")

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    compileOnly(libs.kotlinx.coroutines.slf4j)
    compileOnly(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // Spring Boot
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}
