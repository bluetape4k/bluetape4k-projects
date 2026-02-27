configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
    create("testJar")
}

// 테스트 코드를 Jar로 만들어서 다른 프로젝트에서 참조할 수 있도록 합니다.
tasks.register<Jar>("testJar") {
    dependsOn(tasks.testClasses)
    archiveClassifier.set("test")
    from(sourceSets.test.get().output)
}

artifacts {
    add("testJar", tasks["testJar"])
}

dependencies {
    api(Libs.javers_core)

    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-jackson"))
    implementation(project(":bluetape4k-cache-local"))
    implementation(project(":bluetape4k-grpc"))
    implementation(project(":bluetape4k-hibernate"))
    implementation(project(":bluetape4k-idgenerators"))
    implementation(project(":bluetape4k-redis"))

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Cache for Javers repository
    compileOnly(Libs.caffeine)
    compileOnly(Libs.caffeine_jcache)
    compileOnly(Libs.cache2k_core)

    // Mongo
    compileOnly(Libs.mongo_bson)
    compileOnly(Libs.mongo_bson_kotlin)
    compileOnly(Libs.mongo_bson_kotlinx)
    compileOnly(Libs.mongodb_driver_sync)
    compileOnly(Libs.mongodb_driver_kotlin_sync)
    compileOnly(Libs.mongodb_driver_kotlin_coroutine)
    compileOnly(Libs.mongodb_driver_kotlin_extensions)

    // Codec
    compileOnly(Libs.kryo5)
    compileOnly(Libs.fory_kotlin)

    // Compression
    compileOnly(Libs.lz4_java)
    compileOnly(Libs.snappy_java)
    compileOnly(Libs.zstd_jni)
}
