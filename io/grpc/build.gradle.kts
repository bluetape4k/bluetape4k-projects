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


// 참고: https://github.com/grpc/grpc-kotlin/blob/master/compiler/README.md
protobuf {
    protoc {
        artifact = Libs.protobuf_protoc
    }
    plugins {
        id("grpc") {
            artifact = Libs.grpc_protoc_gen_grpc_java
        }
        id("grpcKt") {
            artifact = Libs.grpc_protoc_gen_grpc_kotlin
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc")
                id("grpcKt")
            }
            task.builtins {
                id("kotlin")
            }
            // DynamicMessage 사용을 위해
            task.generateDescriptorSet = true
            task.descriptorSetOptions.includeSourceInfo = true
            task.descriptorSetOptions.includeImports = true
        }
    }
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-jackson"))
    api(project(":bluetape4k-netty"))
    compileOnly(project(":bluetape4k-money"))
    testImplementation(project(":bluetape4k-junit5"))

    // api(Libs.jakarta_annotation_api)

    api(Libs.grpc_api)
    api(Libs.grpc_alts)
    api(Libs.grpc_netty)
    api(Libs.grpc_protobuf)
    api(Libs.grpc_stub)
    api(Libs.grpc_auth)
    api(Libs.grpc_grpclb)
    api(Libs.grpc_services)
    api(Libs.grpc_inprocess)
    testImplementation(Libs.grpc_okhttp)
    testImplementation(Libs.grpc_testing)

    api(Libs.protobuf_java)
    api(Libs.protobuf_java_util)

    // grpc-kotlin
    // 참고: https://github.com/grpc/grpc-kotlin/blob/master/compiler/README.md
    api(Libs.grpc_kotlin_stub)
    api(Libs.protobuf_kotlin)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Eclipse Collections
    implementation(Libs.eclipse_collections)

    testImplementation(Libs.assertj_core)
}
