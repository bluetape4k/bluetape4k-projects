plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-aws-core"))
    api(project(":bluetape4k-idgenerators"))
    api(project(":bluetape4k-jackson"))
    api(project(":bluetape4k-resilience4j"))
    testImplementation(project(":bluetape4k-junit5"))

    // AWS SDK V2
    api(Libs.aws2_dynamodb_enhanced)
    api(Libs.aws2_netty_nio_client)
    testImplementation(Libs.aws2_test_utils)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Localstack
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_localstack)

    // Spring Boot
    testImplementation(Libs.springBootStarter("aop"))
    testImplementation(Libs.springBootStarter("webflux"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "junit", module = "junit")
    }

    testImplementation(Libs.jakarta_el_api)
    testImplementation(Libs.hibernate_validator)
}
