configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(project(":bluetape4k-logging"))
    testImplementation(project(":bluetape4k-junit5"))
    // ServiceLoader로 등록된 StructuredTaskScopeProvider 구현체를 test runtime에 제공
    testRuntimeOnly(project(":bluetape4k-virtualthread-jdk21"))
}
