plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    compileOnly(project(":bluetape4k-cache"))
    compileOnly(project(":bluetape4k-coroutines"))
    testImplementation(project(":bluetape4k-junit5"))

    // Bucket4j
    api(Libs.bucket4j_core)
    compileOnly(Libs.bucket4j_lettuce)
    compileOnly(Libs.bucket4j_redisson)

    // Local Cache
    compileOnly(Libs.caffeine)

    // Redis
    compileOnly(Libs.lettuce_core)
    compileOnly(Libs.redisson)

    // Coroutines
    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Testcontainers for Redis
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers)

    // Spring Boot Example
    testImplementation(Libs.springBootStarter("webflux"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }

    // Reactor
    testImplementation(Libs.reactor_netty)
    testImplementation(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)
}
