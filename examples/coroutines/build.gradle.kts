configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_slf4j)
    implementation(Libs.kotlinx_coroutines_reactive)
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_debug)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Coroutines Flow를 Reactor처럼 테스트 할 수 있도록 해줍니다.
    // 참고: https://github.com/cashapp/turbine/
    testImplementation(Libs.turbine)

    implementation(Libs.kotlinx_atomicfu)


    // ID Generators
    implementation(project(":bluetape4k-idgenerators"))
    implementation(Libs.java_uuid_generator)

    testImplementation(project(":bluetape4k-junit5"))
}
