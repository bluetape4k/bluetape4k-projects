configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-vertx-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // Vertx
    api(Libs.vertx_core)
    api(Libs.vertx_lang_kotlin)
    api(Libs.vertx_lang_kotlin_coroutines)
    compileOnly(Libs.vertx_web)
    compileOnly(Libs.vertx_web_client)
    compileOnly(Libs.vertx_junit5)

    // Resilience4j
    api(project(":bluetape4k-resilience4j"))
    compileOnly(Libs.resilience4j_reactor)
    compileOnly(Libs.resilience4j_micrometer)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

}
