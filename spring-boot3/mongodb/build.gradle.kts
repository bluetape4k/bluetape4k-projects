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
    implementation(platform(Libs.spring_boot3_dependencies))
    api(project(":bluetape4k-spring-boot3-core"))
    api(project(":bluetape4k-coroutines"))
    testImplementation(project(":bluetape4k-jackson2"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_mongodb)

    // Spring Data MongoDB Reactive
    api(Libs.springBootStarter("data-mongodb-reactive"))

    compileOnly(Libs.springBoot("autoconfigure"))
    compileOnly(Libs.springBoot("configuration-processor"))
    annotationProcessor(Libs.springBoot("configuration-processor"))

    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }

    // Coroutines
    api(Libs.kotlinx_coroutines_core)
    api(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Reactor
    implementation(Libs.reactor_core)
    implementation(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)
}
