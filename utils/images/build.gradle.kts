configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    compileOnly(project(":bluetape4k-coroutines"))
    testImplementation(project(":bluetape4k-junit5"))

    // Images
    // https://mvnrepository.com/artifact/com.sksamuel.scrimage/scrimage-core
    implementation(Libs.scrimage_core)
    implementation(Libs.scrimage_filters)
    implementation(Libs.scrimage_webp)

    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
