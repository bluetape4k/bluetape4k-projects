import com.google.protobuf.gradle.id

plugins {
    `java-library`
    idea
    alias(libs.plugins.protobuf.plugin)
}

idea {
    module {
        sourceDirs.plus(file("${layout.buildDirectory.asFile.get()}/generated/source/proto/main"))
        testSources.plus(file("${layout.buildDirectory.asFile.get()}/generated/source/proto/test"))
    }
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
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
    api(libs.protobuf.java)
    api(libs.protobuf.java.util)
    api(libs.protobuf.kotlin)
    api(libs.proto.google.common.protos)

    api(project(":bluetape4k-io"))

    // Redis
    compileOnly(project(":bluetape4k-lettuce"))
    compileOnly(project(":bluetape4k-redisson"))

    // Redis Codecs
    compileOnly(libs.lz4.java)
    compileOnly(libs.snappy.java)
    compileOnly(libs.zstd.jni)

    // Fallback codec
    // compileOnly(libs.fory.kotlin)

    // Money (MoneySupport.kt)
    compileOnly(project(":bluetape4k-money"))


    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
}
