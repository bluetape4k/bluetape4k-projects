configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-tenant"))
    api(libs.ktor.server.core)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(libs.ktor.server.test.host)
}
