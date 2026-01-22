configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-images"))
    testImplementation(project(":bluetape4k-junit5"))

    // Images
    api(Libs.scrimage_core)
    api(Libs.scrimage_filters)
    implementation(Libs.scrimage_webp)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
