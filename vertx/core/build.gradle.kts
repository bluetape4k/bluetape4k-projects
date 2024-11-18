configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-netty"))
    testImplementation(project(":bluetape4k-junit5"))

    // Vertx
    api(Libs.vertx_core)
    api(Libs.vertx_lang_kotlin)
    api(Libs.vertx_lang_kotlin_coroutines)
    compileOnly(Libs.vertx_web)
    compileOnly(Libs.vertx_web_client)
    compileOnly(Libs.vertx_junit5)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_reactive)
    compileOnly(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

}
