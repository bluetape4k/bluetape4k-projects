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
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-grpc"))
    api(project(":bluetape4k-jackson"))
    api(project(":bluetape4k-idgenerators"))
    compileOnly(project(":bluetape4k-hibernate"))
    compileOnly(project(":bluetape4k-redis"))
    compileOnly(project(":bluetape4k-cache"))

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    api(Libs.javers_core)
    testImplementation(Libs.guava)

    // Cache for Javers repository
    compileOnly(Libs.caffeine)
    compileOnly(Libs.caffeine_jcache)
    compileOnly(Libs.cache2k_core)

    // Mongo
    compileOnly(Libs.mongo_bson)
    compileOnly(Libs.mongodb_driver_sync)

    // Codec
    compileOnly(Libs.kryo)
    compileOnly(Libs.fury_kotlin)

    // Compression
    compileOnly(Libs.lz4_java)
    compileOnly(Libs.snappy_java)
    compileOnly(Libs.zstd_jni)
}
