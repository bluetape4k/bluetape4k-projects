plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.spring.boot3.dependencies))
    api(project(":bluetape4k-r2dbc"))
    testImplementation(project(":bluetape4k-junit5"))

    api(project(":bluetape4k-spring-boot3-core"))

    // R2DBC
    api("org.springframework.boot:spring-boot-starter-data-r2dbc")
    testImplementation(libs.r2dbc.pool)
    testRuntimeOnly(libs.r2dbc.h2)
    testRuntimeOnly(libs.h2.v2)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // PostgreSql Server
    // testImplementation(project(":bluetape4k-testcontainers"))
    // testImplementation(libs.testcontainers.postgresql)
    // testImplementation(libs.r2dbc.postgresql)

    // Spring Boot for Blog Application
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
}
