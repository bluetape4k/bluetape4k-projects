configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    // 일본어 형태소 분석기 (https://mvnrepository.com/artifact/com.atilika.kuromoji/kuromoji-ipadic)
    api(libs.kuromoji.ipadic)
    compileOnly(libs.kuromoji.unidic)

    // bluetape4k
    api(project(":bluetape4k-tokenizer-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
