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
    // JNI native library: isolate each test class in its own JVM fork
    forkEvery = 1
    maxParallelForks = 1
    // Skip libvips-dependent tests when native library is unavailable
    systemProperty("vips.enabled", System.getProperty("vips.enabled", "false"))
}

dependencies {
    api(project(":bluetape4k-images-vips-api"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(testFixtures(project(":bluetape4k-images-vips-api")))

    // JVips JNI bindings (Java 8+; Linux: bundled native / macOS: system libvips required)
    // D8: binding types are internal — use api() only if consumers need VImage directly
    implementation(Libs.jvips)

    // BoundedInputStream for input size limits
    implementation(Libs.commons_io)

    // Coroutines
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
