configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Google Tink - 현대적 AEAD 암호화 (AES-GCM, ChaCha20-Poly1305, AES-SIV, HMAC)
    api(bt4k.tink)

    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Redis
    compileOnly(libs.lettuce.core)
    compileOnly(bt4k.redisson)

    // Coroutines
    testImplementation(project(":bluetape4k-coroutines"))
    testImplementation(libs.kotlinx.coroutines.test)
}
