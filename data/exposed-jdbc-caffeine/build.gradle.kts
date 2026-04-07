configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-exposed-jdbc"))
    api(project(":bluetape4k-exposed-cache"))
    api(Libs.caffeine)

    api(Libs.exposed_core)
    api(Libs.exposed_jdbc)
    compileOnly(Libs.exposed_java_time)
    compileOnly(Libs.exposed_kotlin_datetime)

    api(Libs.kotlinx_coroutines_core)

    testImplementation(testFixtures(project(":bluetape4k-exposed-cache")))
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.hikaricp)
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(Libs.awaitility_kotlin)
}
