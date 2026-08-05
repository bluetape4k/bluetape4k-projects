configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Coroutines
    testImplementation(project(":bluetape4k-coroutines"))
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.slf4j)
    testImplementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.debug)
    testImplementation(libs.kotlinx.coroutines.test)

    // Coroutines Flow를 Reactor처럼 테스트 할 수 있도록 해줍니다.
    // 참고: https://github.com/cashapp/turbine/
    testImplementation(bt4k.turbine)

    // ID Generators
    testImplementation(project(":bluetape4k-idgenerators"))
    testImplementation(bt4k.java.uuid.generator)

    testImplementation(project(":bluetape4k-junit5"))
}
