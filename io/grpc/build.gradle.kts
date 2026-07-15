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


// 참고: https://github.com/grpc/grpc-kotlin/blob/master/compiler/README.md
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${bt4k.versions.protobuf.get()}"
    }
    plugins {
        id("grpc") {
            artifact = libs.grpc.protoc.gen.grpc.java.get().toString()
        }
        id("grpcKt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${libs.versions.grpc.kotlin.get()}:jdk8@jar"
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
    api(project(":bluetape4k-protobuf"))
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-jackson3"))
    api(project(":bluetape4k-netty"))
    testImplementation(project(":bluetape4k-junit5"))

    // api(libs.jakarta.annotation.api)

    api(libs.grpc.api)
    api(libs.grpc.alts)
    api(libs.grpc.netty)
    api(libs.grpc.protobuf)
    api(libs.grpc.stub)
    api(libs.grpc.auth)
    api(libs.grpc.grpclb)
    api(libs.grpc.services)
    api(libs.grpc.inprocess)
    testImplementation(libs.grpc.okhttp)
    testImplementation(libs.grpc.testing)

    // grpc-kotlin
    // 참고: https://github.com/grpc/grpc-kotlin/blob/master/compiler/README.md
    api(libs.grpc.kotlin.stub)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // Eclipse Collections
    implementation(libs.eclipse.collections)

    testImplementation(libs.assertj.core)
}
