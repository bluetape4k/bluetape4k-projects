configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-coroutines"))
    api(project(":bluetape4k-tokenizer-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // twitter-text 의존성 제거: VALID_URL/HASHTAG/MENTION/CASHTAG 패턴은 TwitterCompatPatterns.kt 에서 내부 구현
    // Benchmark 비교를 위해
    testImplementation("org.openkoreantext:open-korean-text:2.3.1")

    // Coroutines
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // Collections
    implementation(libs.commons.collections4)
    implementation(libs.eclipse.collections)
    implementation(libs.eclipse.collections.forkjoin)
    testImplementation(libs.eclipse.collections.testutils)
}
