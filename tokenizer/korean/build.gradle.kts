configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-coroutines"))
    api(project(":bluetape4k-tokenizer-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // 한글 분석을 위한 기본적인 통계 정보를 Twitter에서 제공합니다
    api("com.twitter.twittertext:twitter-text:3.1.0")
    // Benchmark 비교를 위해
    testImplementation("org.openkoreantext:open-korean-text:2.3.1")

    // Coroutines
    api(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
