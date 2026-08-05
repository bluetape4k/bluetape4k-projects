plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

val consumerRuntimeTest by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + configurations.runtimeClasspath.get()
    runtimeClasspath += output + compileClasspath
}

val consumerRuntimeTestImplementation by configurations.getting

tasks.register<Test>("consumerRuntimeTest") {
    description = "Runs Redis serializer smoke tests with the published runtime classpath."
    group = LifecycleBasePlugin.VERIFICATION_GROUP

    testClassesDirs = consumerRuntimeTest.output.classesDirs
    classpath = consumerRuntimeTest.runtimeClasspath
    useJUnitPlatform()
}

tasks.named("check") {
    dependsOn("consumerRuntimeTest")
}

dependencies {
    implementation(platform(bt4k.spring.boot4.dependencies))

    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))

    // Spring Data Redis (4.x BOM에서 버전 해소)
    api("org.springframework.boot:spring-boot-starter-data-redis")

    // Runtime codecs used by the documented RedisBinarySerializers matrix
    runtimeOnly(bt4k.fory.kotlin)
    runtimeOnly(bt4k.kryo5)

    // Runtime compressors used by the documented RedisBinarySerializers matrix
    runtimeOnly(bt4k.at.yawk.lz4.java)
    runtimeOnly(bt4k.zstd.jni)
    runtimeOnly(bt4k.snappy.java)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    consumerRuntimeTestImplementation(project(":bluetape4k-junit5"))
}
