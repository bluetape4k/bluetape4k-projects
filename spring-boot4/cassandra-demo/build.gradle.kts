plugins {
    kotlin("plugin.spring")
    kotlin("plugin.noarg")
    kotlin("plugin.allopen")
    id(Plugins.graalvm_native)
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
    implementation(platform(Libs.spring_boot4_dependencies))
    implementation(project(":bluetape4k-cassandra"))
    implementation(project(":bluetape4k-spring-boot4-cassandra"))
    testImplementation(project(":bluetape4k-jackson3"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // NOTE: Cassandra 4 oss 버전을 사용합니다.
    implementation(Libs.cassandra_java_driver_core)
    implementation(Libs.cassandra_java_driver_query_builder)
    implementation(Libs.cassandra_java_driver_mapper_runtime)
    implementation(Libs.cassandra_java_driver_metrics_micrometer)

    implementation(Libs.springBootStarter("aop"))
    implementation(Libs.springBootStarter("data-cassandra"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactive)
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    implementation(Libs.reactor_core)
    implementation(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)
}
