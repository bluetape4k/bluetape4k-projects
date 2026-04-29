plugins {
    `java-test-fixtures`
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    // Consumers need @IncubatingImageApi annotation transitively
    api(project(":bluetape4k-images"))
    api(project(":bluetape4k-coroutines"))
    testImplementation(project(":bluetape4k-junit5"))

    // Coroutines
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
