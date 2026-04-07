configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-exposed-r2dbc"))
    api(project(":bluetape4k-exposed-cache"))
    api(project(":bluetape4k-coroutines"))
    api(Libs.caffeine)

    api(Libs.exposed_core)
    api(Libs.exposed_r2dbc)
    compileOnly(Libs.exposed_java_time)
    compileOnly(Libs.exposed_kotlin_datetime)

    api(Libs.kotlinx_coroutines_core)
    api(Libs.kotlinx_coroutines_reactive)

    testRuntimeOnly(Libs.r2dbc_h2)
    testImplementation(testFixtures(project(":bluetape4k-exposed-cache")))
    testImplementation(project(":bluetape4k-exposed-r2dbc-tests"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(Libs.awaitility_kotlin)
}
