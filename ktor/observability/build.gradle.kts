configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-ktor-core"))
    api(libs.ktor.server.core)

    compileOnly(project(":bluetape4k-micrometer"))
    compileOnly(libs.micrometer.core)
    testImplementation(libs.micrometer.core)
    testImplementation(libs.micrometer.registry.prometheus)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(libs.ktor.server.test.host)
}
