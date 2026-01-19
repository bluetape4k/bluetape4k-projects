configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // Cryptography
    api(Libs.jasypt)
    api(Libs.bouncycastle_bcprov)
    api(Libs.bouncycastle_bcpkix)

    // Coroutines
    testImplementation(project(":bluetape4k-coroutines"))
    testImplementation(Libs.kotlinx_coroutines_test)

}
