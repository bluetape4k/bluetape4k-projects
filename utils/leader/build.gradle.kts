configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // VirtualFuture, VirtualThreadExecutor 사용을 위해 core 의존성 추가
    api(project(":bluetape4k-core"))

    testImplementation(project(":bluetape4k-junit5"))

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
