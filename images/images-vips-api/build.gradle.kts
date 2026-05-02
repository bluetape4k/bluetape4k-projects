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

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Test Fixtures — VipsGoldenAssert needs scrimage pixel comparison + JUnit5
    testFixturesApi(project(":bluetape4k-images"))
    testFixturesImplementation(project(":bluetape4k-junit5"))
    testFixturesImplementation(libs.junit.jupiter.api)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(libs.kotlinx.coroutines.test)
}
