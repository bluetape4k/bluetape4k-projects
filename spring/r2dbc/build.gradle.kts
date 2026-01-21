plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-r2dbc"))
    testImplementation(project(":bluetape4k-junit5"))

    api(project(":bluetape4k-spring-core"))
    testImplementation(project(":bluetape4k-spring-tests"))

    // R2DBC
    api(Libs.springBootStarter("data-r2dbc"))
    testImplementation(Libs.r2dbc_pool)
    testRuntimeOnly(Libs.r2dbc_h2)
    testRuntimeOnly(Libs.h2_v2)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // PostgreSql Server
    // testImplementation(project(":bluetape4k-testcontainers"))
    // testImplementation(Libs.testcontainers_postgresql)
    // testImplementation(Libs.r2dbc_postgresql)

    // Spring Boot for Blog Application
    testImplementation(Libs.springBootStarter("webflux"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
}
