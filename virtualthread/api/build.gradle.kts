plugins {
    `java-test-fixtures`
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-logging"))

    // junit5는 이 API에 의존하므로 fixture는 순환이 없는 최소 검증 모듈만 사용합니다.
    testFixturesApi(project(":bluetape4k-assertions"))
    testImplementation(project(":bluetape4k-junit5"))
    // Java 21 compatibility island의 test runtime provider
    testRuntimeOnly(project(":bluetape4k-virtualthread-jdk21"))
}
