configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-exposed-r2dbc"))
    api(project(":bluetape4k-exposed-cache"))
    api(project(":bluetape4k-coroutines"))
    api(libs.caffeine)

    api(libs.exposed.core)
    api(libs.exposed.r2dbc)
    compileOnly(libs.exposed.java.time)
    compileOnly(libs.exposed.kotlin.datetime)

    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.reactive)

    testRuntimeOnly(libs.r2dbc.h2)
    testImplementation(testFixtures(project(":bluetape4k-exposed-cache")))
    testImplementation(project(":bluetape4k-exposed-r2dbc-tests"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(libs.h2.v2)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.awaitility.kotlin)
}
