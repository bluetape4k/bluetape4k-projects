dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-coroutines"))
    api(project(":bluetape4k-logging"))
    api(libs.kotlinx.coroutines.core)
    api(bt4k.openfga.sdk)
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-assertions"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(bt4k.mockk)
}

tasks.withType<Test>().configureEach {
    if (providers.gradleProperty("excludeIntegrationTests").orNull == "true") {
        useJUnitPlatform { excludeTags("integration") }
    }
}
