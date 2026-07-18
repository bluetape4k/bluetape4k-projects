plugins {
    kotlin("plugin.allopen")
    alias(libs.plugins.kotlinx.benchmark)
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

sourceSets {
    create("benchmark")
}

kotlin {
    target {
        compilations.getByName("benchmark").associateWith(compilations.getByName("main"))
    }
}

configurations {
    named("benchmarkImplementation") {
        extendsFrom(
            configurations.getByName("implementation"),
            configurations.getByName("compileOnly"),
            configurations.getByName("testImplementation"),
        )
    }
    named("benchmarkRuntimeOnly") {
        extendsFrom(
            configurations.getByName("runtimeOnly"),
            configurations.getByName("testRuntimeOnly"),
        )
    }
}

benchmark {
    targets {
        register("benchmark") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = libs.versions.jmh.get()
        }
    }
}

dependencies {
    implementation(project(":bluetape4k-io"))
    implementation(project(":bluetape4k-json"))
    implementation(project(":bluetape4k-jackson2"))
    implementation(project(":bluetape4k-jackson3"))
    implementation(project(":bluetape4k-fastjson2"))
    implementation(project(":bluetape4k-avro"))
    implementation(libs.kryo5)
    implementation(bt4k.fory.kotlin)

    testImplementation(project(":bluetape4k-junit5"))

    add("benchmarkImplementation", libs.kotlinx.benchmark.runtime)
    add("benchmarkImplementation", libs.kotlinx.benchmark.runtime.jvm)
    add("benchmarkImplementation", libs.jmh.core)
    add("benchmarkRuntimeOnly", libs.logback.classic)
}
