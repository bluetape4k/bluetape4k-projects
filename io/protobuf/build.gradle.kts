import com.google.protobuf.gradle.id

plugins {
    `java-library`
    idea
    id(Plugins.protobuf) version Plugins.Versions.protobuf
}

idea {
    module {
        sourceDirs.plus(file("${layout.buildDirectory.asFile.get()}/generated/source/proto/main"))
        testSources.plus(file("${layout.buildDirectory.asFile.get()}/generated/source/proto/test"))
    }
}

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
    // Protobuf
    api(Libs.protobuf_java)
    api(Libs.protobuf_java_util)
    api(Libs.protobuf_kotlin)
    api(Libs.proto_google_common_protos)

    api(project(":bluetape4k-io"))

    // Redis
    compileOnly(project(":bluetape4k-lettuce"))
    compileOnly(project(":bluetape4k-redisson"))

    // Redis Codecs
    compileOnly(Libs.lz4_java)
    compileOnly(Libs.snappy_java)
    compileOnly(Libs.zstd_jni)

    // Fallback codec
    // compileOnly(Libs.fory_kotlin)

    // Money (MoneySupport.kt)
    compileOnly(project(":bluetape4k-money"))


    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
}
