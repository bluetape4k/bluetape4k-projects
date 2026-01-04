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

// NOTE: implementation 나 runtimeOnly로 지정된 Dependency를 testimplementation 으로도 지정하도록 합니다.
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-coroutines"))
    testImplementation(project(":bluetape4k-jackson"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // NOTE: Cassandra 4 oss 버전을 사용합니다.
    api(Libs.cassandra_java_driver_core)
    api(Libs.cassandra_java_driver_query_builder)
    compileOnly(Libs.cassandra_java_driver_mapper_runtime)
    compileOnly(Libs.cassandra_java_driver_metrics_micrometer)
    testImplementation(Libs.cassandra_java_driver_test_infra)

    // cassandra 의 @Mapper, @Dao 를 활용할 때 사용합니다.
    // 참고: https://docs.datastax.com/en/developer/java-driver/4.13/manual/mapper/
    kapt(Libs.cassandra_java_driver_mapper_processor)
    kaptTest(Libs.cassandra_java_driver_mapper_processor)

    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)
}
