plugins {
    kotlin("plugin.spring")
    id(Plugins.shadow)

    id("de.undercouch.download") version "5.6.0"
}

// OpenTelemetry Java Agent 를 사용할 경우 아래의 Task 를 실행하여 자동으로 다운로드 하도록 합니다.
tasks {
    test {
        // dependsOn("downloadAgent")
        // jvmArgs = listOf("-javaagent:${project.layout.projectDirectory.asFile}/opentelemetry-javaagent.jar")
    }

    // update-otel-agent.sh 를 수동으로 실행하여 OpenTelemetry Java Agent 를 다운로드 받을 수 있습니다.
    // Download the OpenTelemetry java agent and put it in the build directory
    //    task<Download>("downloadAgent") {
    //        src(Libs.opentelemetry_javaagent_remote_path)
    //        dest("${project.layout.buildDirectory.asFile.get()}/${Libs.opentelemetry_javaagent_local_path}")
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
        mavenBom(Libs.opentelemetry_bom)
        mavenBom(Libs.opentelemetry_alpha_bom)
        mavenBom(Libs.opentelemetry_instrumentation_bom_alpha)
    }
}

dependencies {
    api(project(":bluetape4k-io"))
    implementation(project(":bluetape4k-netty"))
    testImplementation(project(":bluetape4k-junit5"))

    // OpenTelemetry
    api(Libs.opentelemetry_api)
    api(Libs.opentelemetry_sdk)
    api(Libs.opentelemetry_extension_kotlin)
    compileOnly(Libs.opentelemetry_sdk_extensions_autoconfigure)
    compileOnly(Libs.opentelemetry_sdk_metrics)
    compileOnly(Libs.opentelemetry_sdk_logs)
    compileOnly(Libs.opentelemetry_sdk_trace)
    compileOnly(Libs.opentelemetry_sdk_testing)

    compileOnly(Libs.opentelemetry_exporter_logging)
    // logback mdc 로 otel 정보를 전달하는 라이브러리
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/logback/logback-mdc-1.0/library
    testRuntimeOnly(Libs.opentelemetry_logback_mdc_1_0)
    // otel 이 jul 을 사용해서 로그를 남기는데, 이를 slf4j 로 전달해주는 라이브러리
    compileOnly(Libs.jul_to_slf4j)

    // Opentelemetry instrumentation for spring boot starter
    implementation(Libs.opentelemetry_spring_boot_starter)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_slf4j)
    compileOnly(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Spring Boot
    testImplementation(Libs.springBootStarter("webflux"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}
