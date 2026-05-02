plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.spring.boot3.dependencies))
    implementation(project(":bluetape4k-spring-boot3-exposed-r2dbc"))

    implementation(libs.exposed.r2dbc)
    implementation(libs.exposed.java.time)

    runtimeOnly(libs.r2dbc.h2)
    runtimeOnly(libs.h2.v2)   // JDBC DataSource (DataInitializer + SchemaUtils에 필요)

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation(libs.jackson.module.kotlin)
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    implementation(project(":bluetape4k-coroutines"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    testImplementation(project(":bluetape4k-junit5"))
}
