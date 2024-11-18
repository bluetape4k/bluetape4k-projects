plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // Jackson
    compileOnly(project(":bluetape4k-jackson"))
    compileOnly(Libs.jackson_module_kotlin)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(Libs.kotlinx_coroutines_core)
    api(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Reactor
    compileOnly(Libs.reactor_core)
    compileOnly(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)

    compileOnly(project(":bluetape4k-spring-core"))
    compileOnly(Libs.springBootStarter("data-r2dbc"))
    compileOnly(Libs.r2dbc_h2)
    compileOnly(Libs.r2dbc_pool)
    compileOnly(Libs.r2dbc_postgresql)
    compileOnly(Libs.r2dbc_mysql_1_3)

    // Spring Boot
    compileOnly(Libs.springBoot("autoconfigure"))

    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
}
