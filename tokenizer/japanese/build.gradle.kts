configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    // 일본어 형태소 분석기 (https://mvnrepository.com/artifact/com.atilika.kuromoji/kuromoji-ipadic)
    val kuromoji = "0.9.0"
    api("com.atilika.kuromoji:kuromoji-ipadic:$kuromoji")
    compileOnly("com.atilika.kuromoji:kuromoji-unidic:$kuromoji")

    // bluetape4k
    api(project(":bluetape4k-tokenizer-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
