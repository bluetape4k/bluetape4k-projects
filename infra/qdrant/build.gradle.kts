configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(bt4k.qdrant.client)

    // Bluetape4k 
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // Testcontainers
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers.qdrant)
}

tasks.withType<Test>().configureEach {
    if (providers.gradleProperty("excludeIntegrationTests").orNull == "true") {
        useJUnitPlatform { excludeTags("integration") }
    }
}
