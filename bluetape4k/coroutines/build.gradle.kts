configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))

    api(Libs.kotlinx_atomicfu)

    // Coroutines
    api(Libs.kotlinx_coroutines_core)
    api(Libs.kotlinx_coroutines_slf4j)
    compileOnly(Libs.kotlinx_coroutines_reactive)
    compileOnly(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_debug)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Collections
    api(Libs.eclipse_collections)
    api(Libs.eclipse_collections_forkjoin)
    testImplementation(Libs.eclipse_collections_testutils)
    compileOnly(Libs.pods4k_core)
    compileOnly(Libs.pods4k_transformations_to_standard_collections)
    compileOnly(Libs.commons_collections4)
    
    // Test Fixture
    compileOnly(Libs.kluent)
    compileOnly(Libs.kotlin_test_junit5)

    testImplementation(Libs.mockk)

    // Coroutines Flow를 Reactor처럼 테스트 할 수 있도록 해줍니다.
    // 참고: https://github.com/cashapp/turbine/
    testImplementation(Libs.turbine)
}
