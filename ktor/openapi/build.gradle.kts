configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-ktor-core"))
    api(libs.ktor.server.core)
    api(libs.ktor.server.openapi)
    api(libs.ktor.server.routing.openapi)
    api(libs.ktor.server.swagger)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-ktor-testing"))
    testImplementation(libs.ktor.server.test.host)
}
