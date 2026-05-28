configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-ktor-core"))
    api(libs.ktor.server.core)
    api(libs.ktor.server.call.id)
    api(libs.ktor.server.call.logging)
    api(libs.ktor.server.metrics.micrometer)
    api(libs.micrometer.core)

    compileOnly(libs.micrometer.registry.prometheus)
    testImplementation(libs.micrometer.registry.prometheus)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(libs.ktor.server.test.host)
}
