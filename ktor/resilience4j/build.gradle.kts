configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-ktor-core"))
    api(project(":bluetape4k-resilience4j"))
    api(libs.ktor.server.core)
    api(libs.ktor.server.status.pages)

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-ktor-testing"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlinx.coroutines.test)
}
