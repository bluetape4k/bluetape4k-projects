plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.spring.boot3.dependencies))
    implementation(project(":bluetape4k-spring-boot3-exposed-jdbc"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(libs.jackson.module.kotlin)
    implementation(libs.exposed.spring.boot.starter)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.migration.jdbc)
    implementation(libs.exposed.java.time)
    runtimeOnly(libs.h2.v2)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
