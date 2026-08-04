plugins {
    kotlin("kapt")
    kotlin("plugin.noarg")
    kotlin("plugin.allopen")
}
allOpen {
    annotation("com.datastax.oss.driver.api.mapper.annotations.Entity")
}
noArg {
    annotation("com.datastax.oss.driver.api.mapper.annotations.Entity")
    invokeInitializers = true
}

kapt {
    correctErrorTypes = true
    showProcessorStats = true
}

// NOTE: implementation 나 runtimeOnly로 지정된 Dependency를 testimplementation 으로도 지정하도록 합니다.
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

val consumerRuntimeTest = sourceSets.create("consumerRuntimeTest") {
    compileClasspath += sourceSets.main.get().output + configurations.runtimeClasspath.get()
    runtimeClasspath += output + compileClasspath
}

tasks.register<Test>("consumerRuntimeTest") {
    description = "Runs Cassandra mapper API smoke tests with the published runtime classpath."
    group = LifecycleBasePlugin.VERIFICATION_GROUP

    testClassesDirs = consumerRuntimeTest.output.classesDirs
    classpath = consumerRuntimeTest.runtimeClasspath
    useJUnitPlatform()
}

tasks.named("check") {
    dependsOn("consumerRuntimeTest")
}

dependencies {
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-coroutines"))
    testImplementation(project(":bluetape4k-jackson3"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // NOTE: Cassandra 4 oss 버전을 사용합니다.
    api(bt4k.cassandra.java.driver.core)
    api(bt4k.cassandra.java.driver.query.builder)
    api(bt4k.cassandra.java.driver.mapper.runtime)
    compileOnly(bt4k.cassandra.java.driver.metrics.micrometer)
    testImplementation(bt4k.cassandra.java.driver.test.infra)

    // cassandra 의 @Mapper, @Dao 를 활용할 때 사용합니다.
    // 참고: https://docs.datastax.com/en/developer/java-driver/4.13/manual/mapper/
    kapt(bt4k.cassandra.java.driver.mapper.processor)
    kaptTest(bt4k.cassandra.java.driver.mapper.processor)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    add("consumerRuntimeTestImplementation", project(":bluetape4k-junit5"))
}
