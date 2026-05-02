plugins {
    kotlin("plugin.spring")
    kotlin("plugin.noarg")
    kotlin("plugin.allopen")
    kotlin("kapt")
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

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.spring.boot4.dependencies))
    api(project(":bluetape4k-cassandra"))
    api(project(":bluetape4k-spring-boot4-core"))
    testImplementation(project(":bluetape4k-jackson3"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // NOTE: Cassandra 4 oss 버전을 사용합니다.
    api(libs.cassandra.java.driver.core)
    api(libs.cassandra.java.driver.query.builder)
    compileOnly(libs.cassandra.java.driver.mapper.runtime)
    compileOnly(libs.cassandra.java.driver.metrics.micrometer)

    // cassandra 의 @Mapper, @Dao 를 활용할 때 사용합니다.
    // 참고: https://docs.datastax.com/en/developer/java-driver/4.13/manual/mapper/
    kapt(libs.cassandra.java.driver.mapper.processor)
    kaptTest(libs.cassandra.java.driver.mapper.processor)

    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.springframework.boot:spring-boot-starter-data-cassandra")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    implementation(libs.reactor.core)
    implementation(libs.reactor.kotlin.extensions)
    testImplementation(libs.reactor.test)
}
