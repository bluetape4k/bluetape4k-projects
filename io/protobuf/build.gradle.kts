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
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))

    // Protobuf
    api(Libs.protobuf_java)
    api(Libs.protobuf_java_util)
    api(Libs.protobuf_kotlin)
    api(Libs.proto_google_common_protos)

    // Money (MoneySupport.kt)
    compileOnly(project(":bluetape4k-money"))

    testImplementation(project(":bluetape4k-junit5"))
}
