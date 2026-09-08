dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-coroutines"))
    api(libs.kotlinx.coroutines.core)
    api(bt4k.qdrant.client)
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-assertions"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers.qdrant)
}

tasks.withType<Test>().configureEach {
    if (providers.gradleProperty("excludeIntegrationTests").orNull == "true") {
        useJUnitPlatform { excludeTags("integration") }
    }
}
