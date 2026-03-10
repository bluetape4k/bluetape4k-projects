import com.google.protobuf.gradle.id

plugins {
    idea
    id(Plugins.protobuf) version Plugins.Versions.protobuf
}

idea {
    module {
        sourceDirs.plus(file("${layout.buildDirectory.asFile.get()}/generated/source/proto/main"))
        testSources.plus(file("${layout.buildDirectory.asFile.get()}/generated/source/proto/test"))
    }
}

// Protobuf Message를 Lettuce Redis에 저장하는 예제를 위해
// 참고: https://github.com/grpc/grpc-kotlin/blob/master/compiler/README.md
protobuf {
    protoc {
        artifact = Libs.protobuf_protoc
    }
    generateProtoTasks {
        all().forEach { task ->
            // DynamicMessage 사용을 위해
            task.generateDescriptorSet = true
            task.descriptorSetOptions.includeSourceInfo = true
            task.descriptorSetOptions.includeImports = true

            task.builtins {
                // Kotlin DSL 생성
                id("kotlin")
            }
        }
    }
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-netty"))

    // Lettuce
    api(Libs.lettuce_core)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Serializer
    compileOnly(Libs.fory_kotlin)
    compileOnly(Libs.kryo5)

    // Compressor
    compileOnly(Libs.lz4_java)
    compileOnly(Libs.snappy_java)
    compileOnly(Libs.zstd_jni)

    // Protobuf
    compileOnly(project(":bluetape4k-grpc"))
    compileOnly(Libs.protobuf_java)
    compileOnly(Libs.protobuf_java_util)
    compileOnly(Libs.protobuf_kotlin)

    // Jackson
    compileOnly(project(":bluetape4k-jackson"))
    compileOnly(Libs.jackson_dataformat_protobuf)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
}
