configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // VirtualFuture, VirtualThreadExecutor 사용을 위해 core 의존성 추가
    api(project(":bluetape4k-core"))

    testImplementation(project(":bluetape4k-junit5"))

    // Coroutines
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
