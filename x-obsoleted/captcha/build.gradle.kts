configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-images"))
    testImplementation(project(":bluetape4k-junit5"))

    // Images
    api(libs.scrimage.core)
    api(libs.scrimage.filters)
    implementation(libs.scrimage.webp)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
