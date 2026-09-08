plugins {
    `java-test-fixtures`
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // junit5는 이 API에 의존하므로 fixture는 순환이 없는 최소 검증 모듈만 사용합니다.
    testFixturesApi(project(":bluetape4k-assertions"))
    implementation(project(":bluetape4k-logging"))
    testImplementation(project(":bluetape4k-junit5"))
    // Java 21 compatibility island의 test runtime provider
    testRuntimeOnly(project(":bluetape4k-virtualthread-jdk21"))
}
