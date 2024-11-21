configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(project(":bluetape4k-io"))
    implementation(project(":bluetape4k-jdbc"))
    testImplementation(project(":bluetape4k-junit5"))

    // Vertx
    api(project(":bluetape4k-vertx-core"))
    testImplementation(Libs.vertx_junit5)

    // Vertx Kotlin
    implementation(Libs.vertx_core)
    implementation(Libs.vertx_lang_kotlin)
    implementation(Libs.vertx_lang_kotlin_coroutines)
    implementation(Libs.vertx_web)
    implementation(Libs.vertx_web_client)

    // Vetx SqlClient Templates 에서 Jackson Databind 를 이용한 매핑을 사용한다
    implementation(project(":bluetape4k-jackson"))
    implementation(Libs.jackson_module_kotlin)
    implementation(Libs.jackson_datatype_jdk8)
    implementation(Libs.jackson_datatype_jsr310)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_jdk8)
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)
}
