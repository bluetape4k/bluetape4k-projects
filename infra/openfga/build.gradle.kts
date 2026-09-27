configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(bt4k.openfga.sdk)

    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-coroutines"))
    testImplementation(project(":bluetape4k-junit5"))

    // Coroutines
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    testImplementation(project(":bluetape4k-io"))
    testImplementation(bt4k.fory.kotlin)

    // Testcontainers
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers.openfga)

    testImplementation(bt4k.mockk)
}

tasks.withType<Test>().configureEach {
    if (providers.gradleProperty("excludeIntegrationTests").orNull == "true") {
        useJUnitPlatform { excludeTags("integration") }
    }
}
