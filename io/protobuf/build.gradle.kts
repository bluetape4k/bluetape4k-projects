import com.google.protobuf.gradle.id

plugins {
    `java-library`
    idea
    alias(bt4k.plugins.protobuf.plugin)
}

idea {
    module {
        sourceDirs.plus(file("${layout.buildDirectory.asFile.get()}/generated/source/proto/main"))
        testSources.plus(file("${layout.buildDirectory.asFile.get()}/generated/source/proto/test"))
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${bt4k.versions.protobuf.get()}"
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
    api(bt4k.protobuf.java)
    api(libs.protobuf.java.util)
    api(libs.protobuf.kotlin)
    api(bt4k.proto.google.common.protos)

    api(project(":bluetape4k-io"))

    // Redis
    compileOnly(project(":bluetape4k-lettuce"))
    compileOnly(project(":bluetape4k-redisson"))

    // Redis Codecs
    compileOnly(bt4k.at.yawk.lz4.java)
    compileOnly(bt4k.snappy.java)
    compileOnly(bt4k.zstd.jni)

    // Fallback codec
    // compileOnly(bt4k.fory.kotlin)

    // Money (MoneySupport.kt)
    compileOnly(project(":bluetape4k-money"))


    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
}
