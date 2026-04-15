plugins {
    kotlin("plugin.spring")
    kotlin("plugin.noarg")
}

noArg {
    annotation("org.springframework.data.mongodb.core.mapping.Document")
    invokeInitializers = true
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.spring_boot4_dependencies))

    api(project(":bluetape4k-spring-boot4-core"))

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_mongodb)

    // Mongo Driver
    implementation(Libs.mongodb_driver_kotlin_sync)
    implementation(Libs.mongodb_driver_kotlin_coroutine)
    implementation(Libs.mongodb_driver_kotlin_extensions)

    // Jackson 3
    testImplementation(project(":bluetape4k-jackson3"))
    testImplementation(Libs.jackson3_module_kotlin)
    testImplementation(Libs.jackson3_module_blackbird)

    // Spring Data MongoDB Reactive
    api(Libs.springBootStarter("data-mongodb-reactive"))

    compileOnly(Libs.springBoot("autoconfigure"))
    compileOnly(Libs.springBoot("configuration-processor"))

    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(Libs.kotlinx_coroutines_core)
    api(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Reactor
    implementation(Libs.reactor_core)
    implementation(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)
}
