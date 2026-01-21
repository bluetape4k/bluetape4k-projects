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

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-cassandra"))
    api(project(":bluetape4k-spring-core"))
    testImplementation(project(":bluetape4k-jackson"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // NOTE: Cassandra 4 oss 버전을 사용합니다.
    api(Libs.cassandra_java_driver_core)
    api(Libs.cassandra_java_driver_query_builder)
    compileOnly(Libs.cassandra_java_driver_mapper_runtime)
    compileOnly(Libs.cassandra_java_driver_metrics_micrometer)

    // cassandra 의 @Mapper, @Dao 를 활용할 때 사용합니다.
    // 참고: https://docs.datastax.com/en/developer/java-driver/4.13/manual/mapper/
    kapt(Libs.cassandra_java_driver_mapper_processor)
    kaptTest(Libs.cassandra_java_driver_mapper_processor)

    compileOnly(Libs.springBoot("autoconfigure"))
    compileOnly(Libs.springBoot("configuration-processor"))
    annotationProcessor(Libs.springBoot("configuration-processor"))

    implementation(Libs.springBootStarter("data-cassandra"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    implementation(Libs.reactor_core)
    implementation(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)
}
