configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    compileOnly(libs.lettuce.core)
    compileOnly(libs.redisson)
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Google Tink - 현대적 AEAD 암호화 (AES-GCM, ChaCha20-Poly1305, AES-SIV, HMAC)
    api(libs.tink)

    // Coroutines
    testImplementation(project(":bluetape4k-coroutines"))
    testImplementation(libs.kotlinx.coroutines.test)
}
