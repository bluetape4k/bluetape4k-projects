configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    jvmTargetValidationMode.set(org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode.WARNING)
}

tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
    // FFM API requires --enable-native-access; JNI native: isolate each test class in its own JVM fork
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    forkEvery = 1
    maxParallelForks = 1
    // 명시적으로 -Dvips.enabled=false/true 를 전달한 경우만 전파한다.
    // 미설정 시 AbstractFfmVipsTest 가 FfmVipsRuntime.init() 결과로 자동 감지한다.
    System.getProperty("vips.enabled")?.let { systemProperty("vips.enabled", it) }
    // macOS (Homebrew): libvips lives in /opt/homebrew/lib which dlopen doesn't find by default.
    // FFM SymbolLookup.libraryLookup uses dlopen, not java.library.path — need DYLD_LIBRARY_PATH.
    val homebrewLib = "/opt/homebrew/lib"
    if (file(homebrewLib).exists()) {
        environment("DYLD_LIBRARY_PATH", homebrewLib)
    }
}

dependencies {
    api(project(":bluetape4k-images-vips-api"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(testFixtures(project(":bluetape4k-images-vips-api")))

    // vips-ffm FFM bindings (JDK 23+; system libvips required on all platforms)
    // D8: binding types are internal — use api() only if consumers need VipsImage directly
    implementation(libs.vips.ffm)

    // BoundedInputStream for input size limits
    implementation(libs.commons.io)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
