configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // Google Tink - 현대적 AEAD 암호화 (AES-GCM, ChaCha20-Poly1305, AES-SIV, HMAC)
    api(Libs.tink)

    // Coroutines
    testImplementation(project(":bluetape4k-coroutines"))
    testImplementation(Libs.kotlinx_coroutines_test)
}
