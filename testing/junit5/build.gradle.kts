configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.junit.bom))

    api(project(":bluetape4k-logging"))
    api(project(":bluetape4k-virtualthread-api"))
    runtimeOnly(project(":bluetape4k-virtualthread-jdk21"))

    api(libs.kotlin.test.junit5)

    api(libs.junit.jupiter)
    api(libs.junit.jupiter.engine)
    api(libs.junit.jupiter.params)
    api(libs.junit.platform.launcher)

    api(project(":bluetape4k-assertions"))
    api(libs.kluent)
    api(libs.mockk)
    api(libs.awaitility.kotlin)

    api(libs.datafaker)
    api(libs.java.uuid.generator)
    api(libs.random.beans)

    api(libs.commons.lang3)
    implementation(libs.logback.classic)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.debug)
    implementation(libs.kotlinx.coroutines.test)

    implementation(libs.eclipse.collections)
    testImplementation(libs.eclipse.collections.testutils)
}
