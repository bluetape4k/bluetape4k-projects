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


// 참고: https://github.com/grpc/grpc-kotlin/blob/master/compiler/README.md
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${bt4k.versions.protobuf.get()}"
    }
    plugins {
        id("grpc") {
            artifact = bt4k.grpc.protoc.gen.grpc.java.get().toString()
        }
        id("grpcKt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${bt4k.versions.managed.grpc.protoc.gen.grpc.kotlin.h8643385749ff.get()}:jdk8@jar"
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

    // api(bt4k.jakarta.annotation.api)

    api(bt4k.grpc.api)
    api(bt4k.grpc.alts)
    api(bt4k.grpc.netty)
    api(bt4k.grpc.protobuf)
    api(bt4k.grpc.stub)
    api(bt4k.grpc.auth)
    api(bt4k.grpc.grpclb)
    api(bt4k.grpc.services)
    api(bt4k.grpc.inprocess)
    testImplementation(bt4k.grpc.okhttp)
    testImplementation(bt4k.grpc.testing)

    // grpc-kotlin
    // 참고: https://github.com/grpc/grpc-kotlin/blob/master/compiler/README.md
    api(bt4k.grpc.kotlin.stub)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // Eclipse Collections
    implementation(bt4k.eclipse.collections)

    testImplementation(libs.assertj.core)
}
