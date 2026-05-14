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

dependencies {
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-coroutines"))
    testImplementation(project(":bluetape4k-jackson3"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // NOTE: Cassandra 4 oss 버전을 사용합니다.
    api(libs.cassandra.java.driver.core)
    api(libs.cassandra.java.driver.query.builder)
    compileOnly(libs.cassandra.java.driver.mapper.runtime)
    compileOnly(libs.cassandra.java.driver.metrics.micrometer)
    testImplementation(libs.cassandra.java.driver.test.infra)

    // cassandra 의 @Mapper, @Dao 를 활용할 때 사용합니다.
    // 참고: https://docs.datastax.com/en/developer/java-driver/4.13/manual/mapper/
    kapt(libs.cassandra.java.driver.mapper.processor)
    kaptTest(libs.cassandra.java.driver.mapper.processor)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)
}
