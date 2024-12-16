plugins {
    kotlin("plugin.allopen")
    id(Plugins.kotlinx_benchmark) version Plugins.Versions.kotlinx_benchmark
}

allOpen {
    // https://github.com/Kotlin/kotlinx-benchmark
    annotation("org.openjdk.jmh.annotations.State")
}
// https://github.com/Kotlin/kotlinx-benchmark
benchmark {
    targets {
        register("test") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = Versions.jmh
        }
    }
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-logging"))
    testImplementation(project(":bluetape4k-junit5"))

    api(Libs.kotlinx_atomicfu)

    // Apache Commons
    api(Libs.commons_lang3)
    api(Libs.commons_codec)
    api(Libs.commons_compress)
    api(Libs.commons_io)

    // Coroutines
    api(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Reactor
    implementation(Libs.reactor_core)
    implementation(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)

    // Pods4k
    compileOnly(Libs.pods4k_core)
    compileOnly(Libs.pods4k_transformations_to_standard_collections)

    // Eclipse Collections
    compileOnly(Libs.eclipse_collections)
    compileOnly(Libs.eclipse_collections_forkjoin)
    testImplementation(Libs.eclipse_collections_testutils)

    // Apple M1
    compileOnly(Libs.jna_platform)

    compileOnly(Libs.netty_transport_native_epoll + ":linux-x86_64")
    compileOnly(Libs.netty_transport_native_kqueue + ":osx-x86_64")
    compileOnly(Libs.netty_transport_native_kqueue + ":osx-aarch_64")

    // Netty 를 Mac M1 에서 사용하기 위한 설정
    compileOnly(Libs.netty_resolver_dns_native_macos + ":osx-aarch_64")

    // Benchmark
    testImplementation(Libs.kotlinx_benchmark_runtime)
    testImplementation(Libs.kotlinx_benchmark_runtime_jvm)
    testImplementation(Libs.jmh_core)
}
