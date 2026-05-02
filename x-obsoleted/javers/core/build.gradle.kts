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
    api("org.javers:javers-core:7.7.0")

    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-jackson2"))
    implementation(project(":bluetape4k-cache-core"))
    implementation(project(":bluetape4k-protobuf"))
    implementation(project(":bluetape4k-hibernate"))
    implementation(project(":bluetape4k-idgenerators"))
    implementation(project(":bluetape4k-redisson"))

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Cache for Javers repository
    compileOnly(libs.caffeine)
    compileOnly(libs.caffeine.jcache)
    compileOnly(libs.cache2k.core)

    // Mongo
    compileOnly(libs.mongo.bson)
    compileOnly(libs.mongo.bson.kotlin)
    compileOnly(libs.mongo.bson.kotlinx)
    compileOnly(libs.mongodb.driver.sync)
    compileOnly(libs.mongodb.driver.kotlin.sync)
    compileOnly(libs.mongodb.driver.kotlin.coroutine)
    compileOnly(libs.mongodb.driver.kotlin.extensions)

    // Codec
    compileOnly(libs.kryo5)
    compileOnly(libs.fory.kotlin)

    // Compression
    compileOnly(libs.lz4.java)
    compileOnly(libs.snappy.java)
    compileOnly(libs.zstd.jni)
}
