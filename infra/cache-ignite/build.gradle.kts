dependencies {
    api(project(":bluetape4k-cache-core"))

    // Apache Ignite 2.x JCache provider
    api(Libs.ignite_core)
    api(Libs.ignite_clients)

    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly("javax.cache:cache-api:1.1.1")
    compileOnly(Libs.kotlinx_coroutines_core)

    testImplementation(testFixtures(project(":bluetape4k-cache-core")))
    testImplementation("javax.cache:cache-api:1.1.1")
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
}
