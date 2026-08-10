configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(bt4k.junit.bom))

    api(project(":bluetape4k-logging"))
    api(project(":bluetape4k-virtualthread-api"))
    // consumer가 실행 JDK에 맞는 provider를 선택하므로 이 모듈 test에서만 JDK 21 provider를 사용한다.
    testRuntimeOnly(project(":bluetape4k-virtualthread-jdk21"))

    api(libs.kotlin.test.junit5)

    api(bt4k.junit.jupiter)
    api(bt4k.junit.jupiter.engine)
    api(bt4k.junit.jupiter.params)
    api(bt4k.junit.platform.launcher)

    api(project(":bluetape4k-assertions"))
    api(bt4k.mockk)
    api(libs.awaitility.kotlin)

    api(bt4k.datafaker)
    api(bt4k.java.uuid.generator)
    api(libs.random.beans)

    api(bt4k.commons.lang3)
    api(bt4k.logback)

    api(libs.kotlinx.coroutines.core)
    compileOnly(libs.kotlinx.coroutines.debug)
    compileOnly(libs.kotlinx.coroutines.test)

    compileOnly(bt4k.eclipse.collections)
}
