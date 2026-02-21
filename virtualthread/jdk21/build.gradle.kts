configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}

dependencies {
    api(project(":bluetape4k-virtualthread-api"))
    implementation(project(":bluetape4k-logging"))
    testImplementation(project(":bluetape4k-junit5"))

    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
