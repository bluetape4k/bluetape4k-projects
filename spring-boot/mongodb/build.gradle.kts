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
    implementation(platform(bt4k.spring.boot4.dependencies))

    api(project(":bluetape4k-spring-boot-core"))

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers.mongodb)

    // Mongo Driver
    implementation(bt4k.mongodb.driver.kotlin.sync)
    implementation(bt4k.mongodb.driver.kotlin.coroutine)
    implementation(bt4k.mongodb.driver.kotlin.extensions)

    // Jackson 3
    testImplementation(project(":bluetape4k-jackson3"))
    testImplementation(libs.jackson3.module.kotlin)
    testImplementation(libs.jackson3.module.blackbird)

    // Spring Data MongoDB Reactive
    api("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")

    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // Reactor
    implementation(libs.reactor.core)
    implementation(libs.reactor.kotlin.extensions)
    testImplementation(libs.reactor.test)
}
