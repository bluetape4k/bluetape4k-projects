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
    // 명시적으로 -Dvips.enabled=false/true 를 전달한 경우만 전파한다.
    // 미설정 시 AbstractJVipsTest 가 JVipsRuntime.init() 결과로 자동 감지한다.
    System.getProperty("vips.enabled")?.let { systemProperty("vips.enabled", it) }
}

dependencies {
    api(project(":bluetape4k-images-vips-api"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(testFixtures(project(":bluetape4k-images-vips-api")))

    // JVips JNI bindings (Java 8+; Linux: bundled native / macOS: system libvips required)
    // D8: binding types are internal — use api() only if consumers need VImage directly
    implementation(libs.jvips)

    // BoundedInputStream for input size limits
    implementation(libs.commons.io)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
