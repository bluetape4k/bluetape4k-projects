dependencies {
    api(project(":bluetape4k-cache-core"))
    api(project(":bluetape4k-cache-local"))
    api(project(":bluetape4k-cache-ignite"))

    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly("javax.cache:cache-api:1.1.1")
    compileOnly(Libs.kotlinx_coroutines_core)

    testImplementation(testFixtures(project(":bluetape4k-cache-core")))
    testImplementation(project(":bluetape4k-cache-local"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
}
